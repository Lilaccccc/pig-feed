package internal.interaction.Service

import internal.auth.enums.account.Role
import internal.feed.service.cache.FeedCacheService
import internal.feed.service.cache.FeedCacheService.addHotScore
import internal.feed.utils.decodeCursor
import internal.infra.enums.*
import internal.infra.errors.*
import internal.infra.rabbitmq.RabbitMQ
import internal.infra.rabbitmq.entity.Event
import internal.interaction.entity.*
import internal.interaction.enums.*
import internal.interaction.enums.ActionType.{Favorite, Like}
import internal.interaction.repository.CommentRepository.WriteCommentResult
import internal.interaction.repository.{ActionRepository, CommentRepository}
import internal.message.enums.MessageType
import internal.message.service.MessageService
import internal.video.repository.VideoStatRepository
import utils.base.ColoredLogger
import utils.base.config.enums.MqConfig

import java.time.Instant

object ActionService extends ColoredLogger {
  private type ActionRequest = (userId: Long, videoId: Long, idempotencyKey: String)
  private type ActionMessage = (userId: Long, videoId: Long, actionType: ActionType, status: ActionStatus)

  // 设置用户对视频的点赞状态为有效。
  def like(request: ActionRequest) = setAction(request.userId, request.videoId, Like, true, request.idempotencyKey)
  // 设置用户对视频的点赞状态为取消。
  def unlike(request: ActionRequest) = setAction(request.userId, request.videoId, Like, false, request.idempotencyKey)
  // 设置用户对视频的收藏状态为有效。
  def favorite(request: ActionRequest) = setAction(request.userId, request.videoId, Favorite, true, request.idempotencyKey)
  
  // 统一处理点赞和收藏状态变更，actionType 区分点赞或收藏，active 表示目标状态。
  private def setAction(userId: Long, videoId: Long, actionType: ActionType, active: Boolean, idempotencyKey: String): Either[Exception, ActionResult] = {
    if userId <= 0 then return Left(ErrInvalidUserID())
    if videoId <= 0 then return Left(ErrInvalidVideoID())
    val idempotencyKeyTrim = idempotencyKey.trim
    if idempotencyKeyTrim.length > MaxIdempotencyKeyLength then return Left(ErrIdempotencyKeyTooLong())

    val initialStat = VideoStatRepository.getVideoStat(videoId)
    if initialStat.isEmpty then return Left(ErrVideoNotFound())
    val actionState = (userId, videoId, active, actionType, idempotencyKeyTrim, initialStat.get)

    def recordActionHotScore(videoId: Long, actionType: ActionType, delta: Long): Unit = if delta != 0L then {
      actionType match {
        case ActionType.Like     => addHotScore(videoId, delta * HotScoreWeight.Like.value, Instant.now)
        case ActionType.Favorite => addHotScore(videoId, delta * HotScoreWeight.Favorite.value, Instant.now)
        case ActionType.Unknown  => ()
      }
    }

    val action = (state: ActionStateResult) => {
      debug(s"state::$state")
      val publish = if state.delta != 0 then {
        val event   = ActionChangedEvent(userId, videoId, actionType, active, idempotencyKey)
        val publish = publishActionChanged(event)
        if publish then {
          val stateActionType = ActionType.value(state.actionType)
          recordActionHotScore(state.videoId, stateActionType, state.delta)
          if stateActionType == ActionType.Like && state.active && state.delta > 0 then {
            val action = (userId = userId, videoId = videoId, actionType = stateActionType, status = ActionStatus.value(state.active))
            notifyLike(action)
          }
        }
        Some(publish)
      } else None

      if publish.nonEmpty && !publish.get then {
        ActionRepository.setAction(userId, videoId, actionType, active, idempotencyKey) match {
          case Left(err) =>
            err match {
              case e: ErrVideoNotFound => Left(err)
              case e: Exception        => Left(ErrUpdateInteractionFailed())
            }
          case Right(value) =>
            value.action match {
              case None         => Left(ErrUpdateInteractionFailed())
              case Some(action) =>
                val actionType = ActionType.value(action.actionType)
                recordActionHotScore(action.videoId, actionType, state.delta)
                if actionType == ActionType.Like && action.active && state.delta > 0 then {
                  notifyLike((userId = userId, videoId = videoId, actionType = actionType, status = ActionStatus.value(active)))
                }
                val likeCount     = if actionType == ActionType.Like then value.count else 0L
                val favoriteCount = if actionType == ActionType.Favorite then value.count else 0L
                val result        = ActionResult(action.videoId, action.actionType, action.active, likeCount, favoriteCount)
                Right(result)
            }
        }
      } else {
        val result = ActionResult(state.videoId, state.actionType, state.active, state.likeCount, state.favoriteCount)
        Right(result)
      }
    }

    debug(s"actionState::$actionState")
    FeedCacheService.setActionState(actionState) match {
      case Left(err)    => Left(err)
      case Right(state) => action(state)
    }
  }

  private def notifyLike(action: ActionMessage) = VideoStatRepository
    .getVideoAuthorId(action.videoId)
    .map(authorId => {
      val eventId = s"interaction:like:${action.videoId}:${action.userId}"
      createInteractionMessage(authorId, MessageType.Like, "收到点赞", "点赞了你的视频", eventId, action.userId)
    })

  private def createInteractionMessage(userId: Long, messageType: MessageType, title: String, content: String, eventId: String, actorId: Long) = {
    VideoStatRepository
      .getUserProfile(actorId)
      .map(actor => MessageService.createFromActorEvent(userId, messageType, title, content, eventId, idempotencyKey = eventId, actorId, actor.nickname, actor.avatarUrl))
  }

  // 设置用户对视频的收藏状态为取消。
  def unFavorite(request: ActionRequest) = setAction(request.userId, request.videoId, Favorite, false, request.idempotencyKey)

  // 使用游标分页查询评论，返回下一页游标和 has_more。cursor 为空字符串表示首页。
  def listComments(videoId: Long, cursor: String, limit: Int): Either[Exception, CommentListResult] = {
    if videoId <= 0 then return Left(ErrInvalidVideoID())
    val queryLimit = limit.normalizeLimit
    val result     = (cursorOpt: Option[CommentCursor]) => {
      // 多查 1 条用于判断是否还有下一页，返回给客户端时再裁掉。
      val raw: List[Comment] = CommentRepository.listComments(videoId, cursorOpt, queryLimit + 1)
      val hasMore            = raw.size > queryLimit
      val list               = if hasMore then raw.take(queryLimit) else raw
      val nextCursor         = list.lastOption.map(last => CommentCursor(last.createdAt, last.id).encodeBase64).getOrElse("")
      Right(CommentListResult(list, nextCursor, hasMore))
    }
    parseCommentCursor(cursor).fold(err => Left(err), cursorOpt => result(cursorOpt))
  }

  // 解析上一页返回的游标，游标内保存最后一条评论的排序字段。
  private def parseCommentCursor(raw: String): Either[Exception, Option[CommentCursor]] = {
    if raw.isBlank then return Right(None)
    val result = (cursor: CommentCursor) => if cursor.commentId <= 0 then Left(ErrInvalidCursor()) else Right(Some(cursor))
    decodeCursor(raw)
      .flatMap(bytes => CommentCursor.decodeBytes(bytes))
      .fold(err => Left(err), result)
  }

  // 创建评论，并通过幂等键防止客户端重试生成重复评论。
  def createComment(userId: Long, videoId: Long, content: String, idempotencyKey: String): Either[Exception, CreateCommentResult] = {
    val idempotencyKeyTrim = idempotencyKey.trim
    if idempotencyKey.length > MaxIdempotencyKeyLength then return Left(ErrIdempotencyKeyTooLong())

    val repoComment = if !idempotencyKeyTrim.isBlank then {
      // 幂等键命中时返回已创建的评论，客户端重试可以拿到同一结果。
      CommentRepository
        .findCommentByUserAndIdempotencyKey(userId, idempotencyKeyTrim)
        .map((comment, count) => CreateCommentResult(comment, count))
    } else None

    if repoComment.nonEmpty then return Right(repoComment.get)

    if videoId <= 0 then return Left(ErrInvalidVideoID())
    if userId <= 0 then return Left(ErrInvalidUserID())

    val contentTrim = content.trim
    if contentTrim.isBlank then return Left(ErrEmptyCommentContent())
    if contentTrim.length > MaxCommentContentLength then return Left(ErrCommentContentTooLong())

    val result = (created: WriteCommentResult) => {
      val comment = created.comment
      val count   = created.count
      val delta   = created.statDelta
      val map     = (c: Comment) => {
        FeedCacheService.addHotScore(c.videoId, delta * HotScoreWeight.Comment.value, Instant.now)
        syncCommentCount(c, count)
        if delta > 0 then notifyComment(c)
        Right(CreateCommentResult(c, count))
      }
      comment.map(map).getOrElse(Left(ErrSaveInteractionFailed()))
    }

    val model = Comment.toCreateComment(videoId, userId, contentTrim, CommentStatus.Normal, idempotencyKeyTrim)
    CommentRepository.createComment(model).fold(err => Left(err), result)
  }

  private def syncCommentCount(comment: Comment, count: Long) = if comment.videoId > 0 then {
    VideoStatRepository
      .getVideoStat(comment.videoId)
      .map(stat => FeedCacheService.setVideoStat(stat.copy(commentCount = count)))
  }

  private def notifyComment(comment: Comment) = VideoStatRepository
    .getVideoAuthorId(comment.videoId)
    .map(authorId => {
      val eventId = s"interaction:comment:${comment.id}"
      createInteractionMessage(authorId, MessageType.Comment, "收到评论", comment.content, eventId, comment.userId)
    })

  // 删除评论并返回删除后的评论状态和视频评论数。
  def deleteComment(commentId: Long, userId: Long, role: Role): Either[Exception, DeleteCommentResult] = {
    if commentId <= 0 then return Left(ErrInvalidCommentID())
    if userId <= 0 then return Left(ErrInvalidUserID())

    val result = (deleted: WriteCommentResult) => {
      val comment = deleted.comment
      val count   = deleted.count
      val delta   = deleted.statDelta
      val map     = (c: Comment) => {
        FeedCacheService.addHotScore(c.videoId, delta * HotScoreWeight.Comment.value, Instant.now)
        syncCommentCount(c, count)
        if delta > 0 then notifyComment(c)
        Right(DeleteCommentResult(c.id, c.status, count))
      }
      comment.map(map).getOrElse(Left(ErrUpdateInteractionFailed()))
    }

    CommentRepository.deleteComment(commentId, userId, role).fold(err => Left(err), result)
  }

  private def publishActionChanged(action: ActionChangedEvent) = {
    val event = Event(MqConfig.interactionExchange, MqConfig.actionChangedQueue, MqConfig.actionChangedRouting, action)
    RabbitMQ.publish(event)
  }
}

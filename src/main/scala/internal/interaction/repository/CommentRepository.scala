package internal.interaction.repository

import internal.auth.enums.account.Role
import internal.infra.utils.clampCount
import internal.interaction.entity.*
import internal.interaction.enums.*
import internal.infra.errors.*
import internal.interaction.sql.*
import internal.video.repository.{VideoModel, VideoRepository, VideoStatModel}
import sqala.jdbc.JdbcTransactionContext
import sqala.metadata.{autoInc, table}
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime
import scala.util.Try

@table("interaction_comment")
final case class CommentModel(
  @autoInc id: Long,
  videoId: Long,
  userId: Long,
  content: String,
  status: Int,
  idempotencyKey: String,
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime]
)

object CommentRepository extends ColoredLogger {
  type WriteCommentResult = (comment: Option[Comment], count: Long, statDelta: Long)

  // 按 created_at 和 id 倒序查询视频评论，支持稳定游标分页。cursor=None 表示首页。
  def listComments(videoId: Long, cursor: Option[CommentCursor], limit: Int): List[Comment] = {
    val sql = listCommentsSql(videoId, cursor, limit, CommentStatus.Normal)
    db.fetchTo[Comment](sql)
  }

  // 软删除评论，并根据操作者身份校验删除权限。
  def deleteComment(commentId: Long, userId: Long, role: Role): Either[Exception, WriteCommentResult] = {
    import sqala.static.dsl.*
    try
      db.executeTransaction {
        // 锁定评论行，避免重复删除时并发扣减 comment_count。
        val commentSql = query(from(CommentModel).filter(_.id == commentId).forUpdate)

        val video = (commentModel: CommentModel) => {
          // 读取视频作者用于权限判断：评论作者、视频作者、管理员都可删除。
          val videoSql = query(from(VideoModel).filter(_.id == commentModel.videoId).forUpdate)
          db.fetch(videoSql).headOption.getOrElse(throw ErrVideoNotFound())
        }

        val delete = (commentModel: CommentModel) => {
          val model = commentModel.copy(status = CommentStatus.Deleted.value, updatedAt = Some(LocalDateTime.now))
          if db.update(model, true) <= 0 then throw ErrDeleteComment()
          (comment = findCommentById(model.id), count = updateVideoStatCounter(model.videoId, CommentCount.Comment, -1L), statDelta = -1L)
        }

        val result = (commentModel: CommentModel, videoModel: VideoModel) =>
          if userId != commentModel.userId && userId != videoModel.authorId && role != Role.Admin then throw ErrCommentPermissionDenied()
          // 已删除评论直接返回当前计数，保持 DELETE 幂等。
          else if commentModel.status == CommentStatus.Deleted.value then {
            val sql = query(from(VideoStatModel).filter(_.videoId == commentModel.videoId).select(_.commentCount).forUpdate)
            (comment = None, count = db.fetch(sql).headOption.getOrElse(0L), statDelta = -1L)
          } else if commentModel.status == CommentStatus.Normal.value then delete(commentModel)
          else throw ErrDeleteComment()

        db.fetch(commentSql).headOption.map(comment => Right(result(comment, video(comment)))).getOrElse(throw ErrCommentNotFound())
      }
    catch {
      case e: Exception => Left(e)
    }
  }

  // 查询评论详情，同时补齐评论用户昵称和头像。
  def findCommentById(commentId: Long)(using JdbcTransactionContext): Option[Comment] = {
    val sql = findCommentByIdSql(commentId)
    db.fetchTo(sql).headOption
  }

  // 锁定 video_stat 后更新计数，避免并发写丢失。
  def updateVideoStatCounter(videoId: Long, field: CommentCount, delta: Long)(using JdbcTransactionContext): Long = {
    val result = (model: VideoStatModel) =>
      field match {
        case CommentCount.Like =>
          val count = clampCount(model.likeCount + delta)
          val copy  = model.copy(likeCount = count, updatedAt = Some(LocalDateTime.now))
          (copy, count)
        case CommentCount.Favorite =>
          val count = clampCount(model.favoriteCount + delta)
          val copy  = model.copy(favoriteCount = count, updatedAt = Some(LocalDateTime.now))
          (copy, count)
        case CommentCount.Comment =>
          val count = clampCount(model.commentCount + delta)
          val copy  = model.copy(commentCount = count, updatedAt = Some(LocalDateTime.now))
          (copy, count)
      }

    import sqala.static.dsl.*
    val sql = query(from(VideoStatModel).filter(_.videoId == videoId).forUpdate)

    val (model, count) = db.fetch(sql).headOption.map(result).getOrElse(throw ErrDeleteComment())

    if db.update(model, true) > 0 then count else throw ErrDeleteComment()
  }

  // 创建评论，并在同一事务内增加视频评论数。
  def createComment(comment: Comment): Either[Exception, WriteCommentResult] = {
    debug(s"comment::$comment")
    import sqala.static.dsl.*

    try
      db.executeTransaction {
        // 评论只能写入已发布视频，锁定视频行可以避免状态变化时写入脏数据。
        val _ = VideoRepository.lockPublishedVideo(comment.videoId)

        val model = comment.toModel.copy(createdAt = Some(LocalDateTime.now), updatedAt = Some(LocalDateTime.now))
        debug(s"model::$model")
        // 唯一键冲突通常表示同一幂等键已创建过评论，交给外层加载已有结果。
        val commentId = Try(db.insertAndReturn(model).id).getOrElse(throw ErrCommentExist())

        val nextCount = updateVideoStatCounter(model.videoId, CommentCount.Comment, 1)

        val result = findCommentById(commentId)
        debug(s"result::$result")
        Right(result
          .map(comment => (comment = Some(comment), count = 1L, statDelta = 1L))
          .getOrElse((comment = None, count = 0L, statDelta = 0L)))
      }
    catch
      case e: ErrCommentExist =>
        error(e.getMessage)
        val result = findCommentByUserAndIdempotencyKey(comment.userId, comment.idempotencyKey)
          .map((comment, count) => (comment = Some(comment), count = count, statDelta = 0L))
          .getOrElse((comment = None, count = 0L, statDelta = 0L))
        Right(result)
      case e: Exception => Left(e)
  }

  // 根据用户和幂等键查找已创建评论。
  def findCommentByUserAndIdempotencyKey(userId: Long, idempotencyKey: String): Option[(comment: Comment, count: Long)] = {
    if idempotencyKey.isBlank then return None.asInstanceOf[Option[(comment: Comment, count: Long)]]
    val sql = findCommentByUserAndIdempotencyKeySql(userId, idempotencyKey.trim)
    db.fetchTo[Comment](sql).headOption.map(c => (comment = c, count = commentCount(c.id)))
  }

  // 读取视频当前评论数，用于幂等评论创建返回一致响应。
  def commentCount(videoId: Long): Long = {
    import sqala.static.dsl.*
    val sql = query(from(VideoStatModel).filter(_.videoId == videoId).select(_.commentCount))
    db.fetch(sql).headOption.getOrElse(0L)
  }
}

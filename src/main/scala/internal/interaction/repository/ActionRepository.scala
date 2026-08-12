package internal.interaction.repository

import internal.infra.enums.*
import internal.infra.errors.*
import internal.interaction.entity.*
import internal.interaction.enums.*
import internal.video.repository.{VideoRepository, VideoStatModel}
import sqala.jdbc.JdbcTransactionContext
import sqala.metadata.{autoInc, table}
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime
import scala.util.Try

// 映射 interaction_action 表，记录用户对视频的点赞/收藏状态。
@table("interaction_action")
final case class ActionModel(
  @autoInc id: Long,
  userId: Long,
  videoId: Long,
  actionType: String,
  status: Int,
  idempotencyKey: Option[String],
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime]
) {
  def toAction = Action(id, userId, videoId, actionType, status, idempotencyKey.map(_.trim).getOrElse(""), createdAt.orNull, updatedAt.orNull)
}

object ActionRepository extends ColoredLogger {
  private type SetActionResult = (action: Option[Action], count: Long, statDelta: Long)

  // 写入点赞或收藏状态，并在同一事务内维护视频统计计数。
  def setAction(userId: Long, videoId: Long, actionType: ActionType, active: Boolean, idempotencyKey: String): Either[Exception, SetActionResult] = {
    import sqala.static.dsl.*

    try
      db.executeTransaction {
        // 先锁定公开视频，保证互动只发生在可互动的视频上。
        val _ = VideoRepository.lockPublishedVideo(videoId)

        // 锁定用户在该视频上的同类行为记录，避免并发请求同时改计数。
        val dsl = from(ActionModel).filter(a => a.userId == userId && a.videoId == videoId && a.actionType == actionType.value)
        val sql = query(dsl.forUpdate)
        val opt = db.fetch(sql).headOption

        val nextStatus = ActionStatus.value(active)

        val time = Some(LocalDateTime.now)

        val (action, delta) = opt match {
          case None =>
            // 首次 DELETE 会创建取消态记录，保证后续 PUT/DELETE 都有稳定幂等基准。
            val action = ActionModel(0L, userId, videoId, actionType.value, nextStatus.value, idempotencyKeyPtr(idempotencyKey), time, time)
            Try(db.insert(action)).getOrElse(throw ErrInsertAction())
            val delta = if active then 1 else 0
            (action = action, delta = delta)
          case Some(action) =>
            // 同一幂等键直接返回当前计数，避免客户端重试重复变更统计。
            val count = if !idempotencyKey.trim.isBlank && action.idempotencyKey.map(_.equals(idempotencyKey)).getOrElse(false) then currentActionCount(videoId, actionType) else 0
            // 只有状态真正变化时才更新 video_stat，重复 PUT 或 DELETE 保持计数稳定。
            val previousStatus         = action.status
            val previousIdempotencyKey = action.idempotencyKey.map(_.trim).getOrElse("")
            val delta                  = if action.status != nextStatus.value then {
              if active then 1 else -1
            } else 0
            val copy = action.copy(status = nextStatus.value, idempotencyKey = idempotencyKeyPtr(idempotencyKey), createdAt = time, updatedAt = time)
            if Try(db.update(copy)).getOrElse(0) <= 0 then throw ErrUpdateActionType()
            (action = copy, delta = delta)
        }

        val count = if delta == 0 then currentActionCount(videoId, actionType) else updateActionStat(videoId, actionType, delta)
        Right((action = Some(action.toAction), count = count, statDelta = delta))
      }
    catch case e: Exception => Left(e)
  }

  // 根据行为类型读取当前统计值。
  private def currentActionCount(videoId: Long, actionType: ActionType)(using JdbcTransactionContext): Long = {
    import sqala.static.dsl.*
    val dsl       = from(VideoStatModel).filter(_.videoId == videoId)
    val sql       = query(dsl)
    val videoStat = db.fetch(sql).headOption.getOrElse(throw ErrVideoNotFound())
    actionType match {
      case ActionType.Like     => videoStat.likeCount
      case ActionType.Favorite => videoStat.favoriteCount
      case ActionType.Unknown  => throw ErrActionType()
    }
  }

  // 将空幂等键存为 NULL，减少唯一索引冲突。
  private def idempotencyKeyPtr(idempotencyKey: String) = if idempotencyKey.trim.equals("") then None else Some(idempotencyKey.trim)

  // 根据行为类型选择要更新的统计字段。
  private def updateActionStat(videoId: Long, actionType: ActionType, delta: Long)(using JdbcTransactionContext): Long = {
    actionType match {
      case ActionType.Like     => CommentRepository.updateVideoStatCounter(videoId, CommentCount.Like, delta)
      case ActionType.Favorite => CommentRepository.updateVideoStatCounter(videoId, CommentCount.Favorite, delta)
      case ActionType.Unknown  => throw ErrActionType()
    }
  }
}

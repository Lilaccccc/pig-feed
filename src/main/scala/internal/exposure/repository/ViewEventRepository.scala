package internal.exposure.repository

import internal.exposure.entity.ViewEvent
import internal.exposure.enums.EventType
import sqala.metadata.*
import utils.base.ColoredLogger

import java.time.LocalDateTime

// 映射 video_view_events 表，保存观看行为流水
@table("video_view_events")
final case class ViewEventModel(
  @autoInc id: Long,
  userId: Long,
  videoId: Long,
  scene: String,
  requestId: Option[String],
  eventType: String,
  watchMs: Int,
  completed: Boolean,
  createdAt: Option[LocalDateTime]
) {
  def restoreViewEvent: ViewEvent = ViewEvent(
    id,
    userId,
    videoId,
    scene,
    requestId.map(_.trim).getOrElse(""),
    EventType.value(eventType),
    watchMs,
    completed,
    createdAt
  )
}

object ViewEventRepository extends ColoredLogger {}

package internal.exposure.entity

import internal.infra.rabbitmq.entity.{EventId, EventTrait}
import io.circe.Codec

import java.time.LocalDateTime

// 观看行为已落库事件，供用户画像和推荐画像 worker 消费
final case class ViewEventRecordedEvent(
  eventId: EventId = EventId.apply(),
  viewEventId: Long,
  userId: Long,
  videoId: Long,
  scene: String,
  requestId: String,
  eventType: String,
  watchMs: Int,
  completed: Boolean,
  recordedAt: LocalDateTime,
  exposureCount: Int,
  occurredAt: LocalDateTime = LocalDateTime.now
) extends EventTrait derives Codec

object ViewEventRecordedEvent {
  def newViewEventRecordedEvent(event: ViewEvent, exposure: Exposure): ViewEventRecordedEvent = ViewEventRecordedEvent(
    viewEventId = event.id,
    userId = event.userId,
    videoId = event.videoId,
    scene = event.scene,
    requestId = event.requestId,
    eventType = event.eventType.value,
    watchMs = event.watchMs,
    completed = event.completed,
    recordedAt = event.createdAt.get,
    exposureCount = exposure.exposureCount
  )
}

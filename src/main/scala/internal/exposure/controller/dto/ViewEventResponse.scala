package internal.exposure.controller.dto

import internal.exposure.entity.ViewEvent
import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

// 观看行为流水响应
final case class ViewEventResponse(
  id: Long,
  userId: Long,
  videoId: Long,
  scene: String,
  requestId: String,
  eventType: String,
  watchMs: Int,
  completed: Boolean,
  createdAt: LocalDateTime
) derives Codec, Schema

object ViewEventResponse {
  extension (viewEvent: ViewEvent) {
    def responseFromResult: ViewEventResponse = ViewEventResponse(
      viewEvent.id,
      viewEvent.userId,
      viewEvent.videoId,
      viewEvent.scene,
      viewEvent.requestId,
      viewEvent.eventType.value,
      viewEvent.watchMs,
      viewEvent.completed,
      viewEvent.createdAt.orNull
    )
  }
}

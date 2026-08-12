package internal.interaction.entity

import internal.infra.rabbitmq.entity.{EventId, EventTrait}
import internal.interaction.enums.ActionType
import io.circe.Codec

import java.time.LocalDateTime

final case class ActionChangedEvent(
  eventId: EventId = EventId.apply(),
  userId: Long,
  videoId: Long,
  actionType: String,
  active: Boolean,
  idempotencyKey: String,
  occurredAt: LocalDateTime = LocalDateTime.now
) extends EventTrait derives Codec

object ActionChangedEvent {
  def apply(userId: Long, videoId: Long, actionType: ActionType, active: Boolean, idempotencyKey: String): ActionChangedEvent = {
    ActionChangedEvent(userId = userId, videoId = videoId, actionType = actionType.value, active = active, idempotencyKey = idempotencyKey.trim)
  }
}

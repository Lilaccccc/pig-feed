package internal.message.controller.dto

import internal.message.entity.Message
import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

final case class MessageResponse(
  id: Long,
  userId: Long,
  messageType: String,
  title: String,
  eventId: String,
  actorId: Long,
  actorNickname: String,
  actorAvatarUrl: String,
  isRead: Boolean,
  createdAt: Option[LocalDateTime],
  readAt: Option[LocalDateTime]
) derives Codec, Schema

object MessageResponse {
  extension (message: Message) {
    def responseFromDomain = MessageResponse(
      message.id.getOrElse(0L),
      message.userId,
      message.messageType,
      message.title,
      message.eventId,
      message.actorId.getOrElse(0L),
      message.actorNickname.orNull,
      message.actorAvatarUrl.orNull,
      message.isRead,
      message.createdAt,
      message.readAt
    )
  }
}

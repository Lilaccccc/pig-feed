package internal.message.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

final case class CreateMessageRequest(
  userId: Long,
  messageType: String,
  title: String,
  content: String,
  eventId: String,
  actorId: Long,
  actorNickname: String,
  actorAvatarUrl: String
) derives Codec, Schema

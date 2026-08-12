package internal.message.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

final case class UnreadStatResponse(unreadCount: Long) derives Codec, Schema

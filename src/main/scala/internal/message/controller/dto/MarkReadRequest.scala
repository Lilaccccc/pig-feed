package internal.message.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

final case class MarkReadRequest(
  messageIds: List[Long]
) derives Codec, Schema

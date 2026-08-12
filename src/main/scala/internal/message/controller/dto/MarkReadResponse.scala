package internal.message.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

final case class MarkReadResponse(updatedCount: Long) derives Codec, Schema

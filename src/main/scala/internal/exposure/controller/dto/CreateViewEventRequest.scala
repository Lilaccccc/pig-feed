package internal.exposure.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

final case class CreateViewEventRequest(
  videoId: Long,
  scene: String,
  requestId: String,
  eventType: String,
  watchMs: Int,
  completed: Boolean
) derives Codec, Schema

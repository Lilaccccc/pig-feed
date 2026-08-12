package internal.exposure.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

final case class ExposureItemResponse(
  userId: Long,
  videoId: Long,
  firstExposedAt: LocalDateTime,
  lastExposedAt: LocalDateTime,
  exposureCount: Int,
  lastScene: String
) derives Codec, Schema

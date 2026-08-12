package internal.exposure.controller.dto

import internal.exposure.entity.ExposureDecision
import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

final case class ExposureDecisionItemResponse(
  videoId: Long,
  allowed: Boolean,
  reason: String,
  lastExposedAt: Option[LocalDateTime]
) derives Codec, Schema

object ExposureDecisionItemResponse {
  extension (e: ExposureDecision)  {
    def exposureDecisionsItemResponseFromResult: ExposureDecisionItemResponse = ExposureDecisionItemResponse(
      e.videoId,
      e.allowed,
      e.reason,
      e.lastExposedAt
    )
  }
}

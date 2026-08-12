package internal.exposure.controller.dto

import internal.exposure.controller.dto.ExposureDecisionItemResponse.exposureDecisionsItemResponseFromResult
import internal.exposure.entity.ExposureDecisionResult
import io.circe.Codec
import sttp.tapir.Schema

final case class ExposureDecisionsResponse(
  userId: Long,
  scene: String,
  requestId: String,
  decisions: List[ExposureDecisionItemResponse]
) derives Codec, Schema

object ExposureDecisionsResponse {
  extension (e: ExposureDecisionResult) {
    def exposureDecisionsResponseFromResult: ExposureDecisionsResponse = ExposureDecisionsResponse(
      e.userId,
      e.scene,
      e.requestId,
      e.decisions.map(_.exposureDecisionsItemResponseFromResult)
    )
  }
}

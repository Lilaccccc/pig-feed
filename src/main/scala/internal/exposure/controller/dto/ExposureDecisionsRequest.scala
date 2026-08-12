package internal.exposure.controller.dto

import internal.exposure.entity.ExposureDecisionInput
import io.circe.Codec
import sttp.tapir.Schema

final case class ExposureDecisionsRequest(
  userId: Long,
  scene: String,
  requestId: String,
  videoIds: List[Long]
) derives Codec, Schema {
  def toInput: ExposureDecisionInput = ExposureDecisionInput(
    userId,
    scene,
    requestId,
    videoIds
  )
}

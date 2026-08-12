package internal.exposure.controller.dto

import internal.exposure.entity.ExposureInput
import io.circe.Codec
import sttp.tapir.Schema

final case class ExposuresRequest(
  userId: Long,
  scene: String,
  requestId: String,
  videoIds: List[Long]
) derives Codec, Schema {
  def toInput: List[ExposureInput] = videoIds.map(v =>
    ExposureInput(
      userId,
      v,
      scene,
      requestId
    )
  )
}

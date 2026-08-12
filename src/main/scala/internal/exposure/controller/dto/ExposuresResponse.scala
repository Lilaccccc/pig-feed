package internal.exposure.controller.dto

import internal.exposure.entity.ExposureResult
import io.circe.Codec
import sttp.tapir.Schema

final case class ExposuresResponse(exposures: List[ExposureItemResponse]) derives Codec, Schema

object ExposuresResponse {
  extension (e: ExposureResult) {
    def exposuresResponseFromResult: List[ExposureItemResponse] = e.exposures.map(e =>
      ExposureItemResponse(
        e.userId,
        e.videoId,
        e.firstExposedAt,
        e.lastExposedAt,
        e.exposureCount,
        e.lastScene
      )
    )
  }
}

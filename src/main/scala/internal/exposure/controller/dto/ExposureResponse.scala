package internal.exposure.controller.dto

import internal.exposure.entity.Exposure
import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

// 曝光聚合响应，exposed 事件会返回该对象
final case class ExposureResponse(
  userId: Long,
  videoId: Long,
  firstExposedAt: LocalDateTime,
  lastExposedAt: LocalDateTime,
  exposureCount: Int,
  lastScene: String
) derives Codec, Schema

object ExposureResponse {
  extension (exposure: Option[Exposure]) {
    def responseFromResult: Option[ExposureResponse] = exposure.map(e =>
      ExposureResponse(
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

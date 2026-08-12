package internal.exposure.controller.dto

import internal.exposure.controller.dto.ExposureResponse.responseFromResult
import internal.exposure.controller.dto.ViewEventResponse.responseFromResult
import internal.exposure.entity.RecordViewEventResult
import io.circe.Codec
import sttp.tapir.Schema

// 上报后的完整响应
final case class CreateViewEventResponse(
  event: ViewEventResponse,
  exposure: Option[ExposureResponse],
  published: Boolean
) derives Codec, Schema

object CreateViewEventResponse {
  def responseFromResult(result: RecordViewEventResult): CreateViewEventResponse = CreateViewEventResponse(
    result.event.responseFromResult,
    result.exposure.responseFromResult,
    result.published
  )
}

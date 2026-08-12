package internal.exposure.controller.dto

import internal.exposure.controller.dto.CandidateItemResponse.candidateItemResponseFromResult
import internal.exposure.entity.CandidateResult
import io.circe.Codec
import sttp.tapir.Schema

final case class CandidateResponse(
  userId: Long,
  scene: String,
  requestId: String,
  candidates: List[CandidateItemResponse],
  nextCursor: String,
  hasMore: Boolean
) derives Codec, Schema

object CandidateResponse {
  extension (result: CandidateResult) {
    def candidateResponseFromResult: CandidateResponse = {
      val items = result.candidates.candidateItemResponseFromResult
      CandidateResponse(
        result.userId,
        result.scene,
        result.requestId,
        items,
        result.nextCursor,
        result.hasMore
      )
    }
  }
}

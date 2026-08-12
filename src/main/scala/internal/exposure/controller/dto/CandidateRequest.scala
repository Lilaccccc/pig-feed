package internal.exposure.controller.dto

import internal.exposure.entity.CandidateInput
import io.circe.Codec
import sttp.tapir.Schema

final case class CandidateRequest(
  userId: Long,
  scene: String,
  requestId: String,
  cursor: String,
  limit: Int
) derives Codec, Schema {
  def toInput: CandidateInput = CandidateInput(userId, scene, requestId, cursor, limit)
}

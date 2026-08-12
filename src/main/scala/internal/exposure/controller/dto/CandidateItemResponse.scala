package internal.exposure.controller.dto

import internal.exposure.entity.Candidate
import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

final case class CandidateItemResponse(
  videoId: Long,
  authorId: Long,
  rankScore: Double,
  similarity: Double,
  hotScore: Int,
  freshnessScore: Double,
  reason: String,
  publishedAt: LocalDateTime
) derives Codec, Schema

object CandidateItemResponse {
  extension (candidates: List[Candidate]) {
    def candidateItemResponseFromResult: List[CandidateItemResponse] = {
      val toResponse = (c: Candidate) => CandidateItemResponse(c.videoId, c.authorId, c.rankScore, c.similarity, c.hotScore, c.freshnessScore, c.reason, c.publishedAt)
      candidates.map(toResponse)
    }
  }
}

package internal.exposure.entity

import java.time.LocalDateTime

final case class Candidate(
  videoId: Long,
  authorId: Long,
  rankScore: Double,
  similarity: Double,
  hotScore: Int,
  freshnessScore: Double,
  reason: String,
  publishedAt: LocalDateTime
)

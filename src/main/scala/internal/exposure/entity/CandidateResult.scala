package internal.exposure.entity

final case class CandidateResult(
  userId: Long,
  scene: String,
  requestId: String,
  candidates: List[Candidate],
  nextCursor: String,
  hasMore: Boolean
)

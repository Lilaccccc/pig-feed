package internal.exposure.entity

final case class CandidateInput(
  userId: Long,
  scene: String,
  requestId: String,
  cursor: String,
  limit: Int
)

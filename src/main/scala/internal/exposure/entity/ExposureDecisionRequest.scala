package internal.exposure.entity

final case class ExposureDecisionRequest(
  userId: Long,
  scene: String,
  requestId: String,
  videoIds: List[Long]
)

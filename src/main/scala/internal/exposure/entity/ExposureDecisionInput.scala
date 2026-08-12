package internal.exposure.entity

case class ExposureDecisionInput(
  userId: Long,
  scene: String,
  requestId: String,
  videoIds: List[Long]
)

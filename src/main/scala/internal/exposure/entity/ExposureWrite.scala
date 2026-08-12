package internal.exposure.entity

final case class ExposureWrite(
  userId: Long,
  videoId: Long,
  scene: String,
  requestId: String
)

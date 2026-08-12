package internal.exposure.entity

import internal.exposure.enums.*

case class ExposureInput(
  userId: Long,
  videoId: Long,
  scene: String,
  requestId: String
) {
  def newExposureWrite: Option[ExposureWrite] = {
    if userId <= 0L || videoId <= 0L then return None
    val sceneLowerTrim = scene.trim.toLowerCase
    val requestIdTrim  = requestId.trim
    if sceneLowerTrim.isBlank || sceneLowerTrim.length > MaxSceneLength || requestIdTrim.length > MaxRequestIDLength then return None
    Some(ExposureWrite(userId, videoId, sceneLowerTrim, requestIdTrim))
  }
}

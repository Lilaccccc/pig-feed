package internal.exposure.entity

import internal.exposure.controller.dto.ExposureDecisionsResponse

case class ExposureDecisionResult(
  userId: Long,
  scene: String,
  requestId: String,
  decisions: List[ExposureDecision]
)

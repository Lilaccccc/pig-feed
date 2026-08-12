package internal.exposure.entity

import internal.exposure.controller.dto.ExposureDecisionItemResponse

import java.time.LocalDateTime

final case class ExposureDecision(
  videoId: Long,
  allowed: Boolean,
  reason: String,
  lastExposedAt: Option[LocalDateTime]
)

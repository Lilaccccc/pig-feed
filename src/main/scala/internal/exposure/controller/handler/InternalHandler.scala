package internal.exposure.controller.handler

import internal.exposure.controller.dto.*
import internal.exposure.controller.dto.CandidateResponse.candidateResponseFromResult
import internal.exposure.controller.dto.ExposureDecisionsResponse.exposureDecisionsResponseFromResult
import internal.exposure.controller.dto.ExposuresResponse.exposuresResponseFromResult
import internal.exposure.service.{ExposureService, RecommendationService}
import utils.base.ColoredLogger
import utils.result.throws

object InternalHandler extends ColoredLogger {
  def listCandidates(request: CandidateRequest): CandidateResponse = RecommendationService.recommend(request.toInput).fold(_.throws(using this), _.candidateResponseFromResult)

  def decideExposures(request: ExposureDecisionsRequest): ExposureDecisionsResponse = ExposureService.decideExposures(request.toInput).fold(_.throws(using this), _.exposureDecisionsResponseFromResult)

  def saveExposures(request: ExposuresRequest): ExposuresResponse = ExposureService
    .saveExposures(request.toInput)
    .fold(_.throws(using this), result => ExposuresResponse(result.exposuresResponseFromResult))
}

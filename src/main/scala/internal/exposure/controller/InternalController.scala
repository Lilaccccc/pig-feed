package internal.exposure.controller

import internal.exposure.controller.dto.*
import internal.exposure.controller.handler.InternalHandler
import internal.infra.route.RouteOps.*
import utils.result.R
import utils.route.Controller

class InternalController extends Controller {
  override val serverEndpointList = List(candidates, decisions, exposures)

  // /internal/recommendation/candidates
  // 一次完成召回、排序、打散
  private def candidates = this.endpoint.post
    .in("internal" / "recommendation" / "candidates")
    .in(jsonBody[CandidateRequest])
    .out(jsonBody[R[CandidateResponse]])
    .logic(request => InternalHandler.listCandidates(request))

  // /internal/exposure/decisions
  // 判断候选是否近期曝光
  private def decisions = this.endpoint.post
    .in("internal" / "exposure" / "decisions")
    .in(jsonBody[ExposureDecisionsRequest])
    .out(jsonBody[R[ExposureDecisionsResponse]])
    .logic(request => InternalHandler.decideExposures(request))

  // /internal/exposures
  // 写入曝光记录
  private def exposures = this.endpoint.post
    .in("internal" / "exposures")
    .in(jsonBody[ExposuresRequest])
    .out(jsonBody[R[ExposuresResponse]])
    .logic(request => InternalHandler.saveExposures(request))
}

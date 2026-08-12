package internal.exposure.controller

import internal.exposure.controller.dto.*
import internal.exposure.controller.handler.ExposureHandler
import internal.infra.route.RouteOps.*
import utils.result.R
import utils.route.Controller

class ExposureController extends Controller {
  override val serverEndpointList = List(createViewEvent)

  // /exposure/video/view/events
  private def createViewEvent = this.endpoint.post
    .in("exposure" / "video" / "view" / "events" and jsonBody[CreateViewEventRequest])
    .out(jsonBody[R[CreateViewEventResponse]])
    .security((c: Context, body: CreateViewEventRequest) => ExposureHandler.createViewEvent(c.userId, body))
}

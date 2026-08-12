package internal.exposure.controller.handler

import internal.exposure.controller.dto.*
import internal.exposure.enums.EventType
import internal.exposure.service.ExposureService
import utils.base.ColoredLogger
import utils.result.throws

object ExposureHandler extends ColoredLogger {
  // 处理视频曝光和观看行为上报
  def createViewEvent(userId: Long, request: CreateViewEventRequest): CreateViewEventResponse = ExposureService
    .recordViewEvent((userId, request.videoId, request.scene, request.requestId, EventType.value(request.eventType), request.watchMs, request.completed))
    .fold(err => err.throws(using this), CreateViewEventResponse.responseFromResult)
}

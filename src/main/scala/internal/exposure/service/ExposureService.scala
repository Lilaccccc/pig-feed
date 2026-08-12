package internal.exposure.service

import internal.exposure.entity.*
import internal.exposure.enums.*
import internal.exposure.enums.EventType.Unknown
import internal.exposure.repository.ExposureRepository
import internal.infra.errors.*
import internal.infra.rabbitmq.RabbitMQ
import internal.infra.rabbitmq.entity.Event
import utils.base.ColoredLogger
import utils.base.config.enums.MqConfig.*

import java.time.LocalDateTime

object ExposureService extends ColoredLogger {
  private type RecordViewEventParam = (userId: Long, videoId: Long, scene: String, requestId: String, eventType: EventType, watchMs: Int, completed: Boolean)

  // 写入观看行为，并在 exposed 事件时同步维护曝光聚合索引。
  def recordViewEvent(param: RecordViewEventParam): Either[Exception, RecordViewEventResult] = {
    val userId = param.userId
    if userId <= 0L then return Left(ErrInvalidUserID())
    val videoId = param.videoId
    if videoId <= 0L then return Left(ErrInvalidVideoID())

    val scene = param.scene.trim
    if scene.isBlank then return Left(ErrEmptyScene())
    if scene.length > MaxSceneLength then return Left(ErrSceneTooLong())
    val requestId = param.requestId.trim
    if requestId.length > MaxRequestIDLength then return Left(ErrRequestIDTooLong())
    val watchMs = param.watchMs
    if watchMs < 0 then return Left(ErrWatchMsNegative())
    val eventType = param.eventType
    if eventType == Unknown then return Left(ErrInvalidEventType())

    val event = ViewEvent(0L, userId, videoId, scene, requestId, eventType, watchMs, param.completed || eventType == EventType.Complete)
    ExposureRepository
      .saveViewEvent(event)
      .fold(
        err => Left(err),
        (savedEvent, exposure) => {
          val published = exposure.map { entity =>
            val message = ViewEventRecordedEvent.newViewEventRecordedEvent(savedEvent, entity)
            val mqEvent = Event(exposureExchange, viewEventRecordedQueue, viewEventRecordedRouting, message)
            // TODO：处理事件
            RabbitMQ.publish(mqEvent)
          }.getOrElse {
            info("RecordViewEvent 事件未推送！")
            false
          }
          Right(RecordViewEventResult(savedEvent, exposure, published))
        }
      )
  }

  def saveExposures(inputs: List[ExposureInput]): Either[Exception, ExposureResult] = {
    val writes = inputs.flatMap(_.newExposureWrite).distinctBy(_.videoId)
    if writes.isEmpty then return Right(ExposureResult(List.empty))
    ExposureRepository
      .saveExposures(writes)
      .fold(err => Left(ErrSaveRecommendationExposureFailed()), exposures => Right(ExposureResult(exposures)))
  }

  def decideExposures(input: ExposureDecisionInput): Either[Exception, ExposureDecisionResult] = {
    val userId    = input.userId
    val scene     = input.scene.trim.toLowerCase
    val requestId = input.requestId.trim
    val videoIds  = input.videoIds.filter(_ > 0L).distinct

    if userId <= 0L then return Left(ErrInvalidUserID())
    if scene.isBlank then return Left(ErrEmptyScene())
    if scene.length > MaxSceneLength then return Left(ErrSceneTooLong())
    if requestId.length > MaxRequestIDLength then return Left(ErrRequestIDTooLong())
    if videoIds.isEmpty then return Right(ExposureDecisionResult(userId, scene, requestId, decisions = List.empty))

    val exposureByVideoId = ExposureRepository.listRecentExposures(userId, videoIds, since = LocalDateTime.now.minusSeconds(RecentExposureWindow)).map(e => (e.videoId, e)).toMap
    val decisions         = videoIds.map { v =>
      exposureByVideoId.get(v) match
        case Some(e) => ExposureDecision(v, false, ExposureDecisionReason.RecentlyExposed.value, Some(e.lastExposedAt))
        case None    => ExposureDecision(v, true, ExposureDecisionReason.Fresh.value, None)
    }
    Right(ExposureDecisionResult(userId, scene, requestId, decisions))
  }
}

package internal.exposure.repository

import internal.exposure.entity.*
import internal.exposure.enums.*
import internal.exposure.sql.saveExposuresSql
import internal.infra.errors.*
import internal.video.repository.VideoRepository
import sqala.metadata.*
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime

// 映射 exposures 表，保存用户看过视频的聚合索引
@table("exposures")
final case class ExposureModel(
  @autoInc id: Long,
  userId: Long,
  videoId: Long,
  firstExposedAt: LocalDateTime,
  lastExposedAt: LocalDateTime,
  exposureCount: Int,
  lastScene: String,
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime]
) {
  def restoreExposure: Exposure = Exposure(id, userId, videoId, firstExposedAt, lastExposedAt, exposureCount, lastScene, createdAt, updatedAt)
}

object ExposureRepository extends ColoredLogger {
  private type SaveViewEventResult = (event: ViewEvent, exposure: Option[Exposure])

  // 写入观看行为，并在 exposed 事件时 upsert 用户视频曝光聚合。
  def saveViewEvent(event: ViewEvent): Either[Exception, SaveViewEventResult] = {
    VideoRepository.findById(event.videoId) match {
      case Left(err)    => return Left(err)
      case Right(value) => ()
    }

    val todo = () =>
      db.executeTransaction {
        val now           = LocalDateTime.now
        val eventModel    = ViewEventModel(0L, event.userId, event.videoId, event.scene, event.requestIdPtr, event.eventType.value, event.watchMs, event.completed, Some(now))
        val exposureModel = ExposureModel(0L, event.userId, event.videoId, now, now, 1, event.scene, Some(now), Some(now))
        val eventEntity   = db.insertAndReturn(eventModel)
        if eventEntity.id <= 0L then throw ErrSaveRecommendationExposureFailed()
        val exposureEntity = if event.countsAsExposure then {
          import sqala.static.dsl.*
          val exist = query(from(ExposureModel).filter(e => e.userId == event.userId && e.videoId == event.videoId))
          val temp  = db.fetch(exist).headOption match {
            case Some(value) => value.copy(lastExposedAt = now, exposureCount = value.exposureCount + 1, lastScene = event.scene, updatedAt = Some(now))
            case None        => exposureModel
          }
          if temp.id == 0L then {
            val save = db.insertAndReturn(temp)
            if save.id <= 0L then throw ErrSaveRecommendationExposureFailed()
            Some(save)
          } else {
            val save = db.update(temp, true)
            if save <= 0L then throw ErrSaveRecommendationExposureFailed()
            Some(temp)
          }
        } else None
        (eventEntity = eventEntity.restoreViewEvent, exposureEntity = exposureEntity.map(_.restoreExposure))
      }

    try {
      val (eventEntity, exposureEntity) = todo()
      Right((event = eventEntity, exposure = exposureEntity))
    } catch case e: Exception => Left(e)
  }

  def listRecentExposures(userId: Long, videoIds: List[Long], since: LocalDateTime): List[Exposure] = {
    if videoIds.isEmpty then return List.empty
    import sqala.static.dsl.*
    val dsl = from(ExposureModel).filter(e => e.userId == userId && e.videoId.in(videoIds) && e.lastExposedAt >= since)
    db.fetch(query(dsl)).map(_.restoreExposure)
  }

  def saveExposures(writes: List[ExposureWrite]): Either[Exception, List[Exposure]] = {
    if writes.isEmpty then return Right(List.empty)
    val now                 = LocalDateTime.now
    val (userIds, videoIds) = writes.map(w => (w.userId, w.videoId)).unzip
    val ensureVideoMap      = VideoRepository.findExistsMapById(videoIds)
    val toMap               = (write: ExposureWrite) => {
      val videoId = write.videoId
      val ensure  = ensureVideoMap.getOrElse(videoId, false)
      if ensure then {
        val userId  = write.userId
        val videoId = write.videoId
        val scene   = write.scene
        val event   = ViewEventModel(0L, userId, videoId, scene, Option(write.requestId), EventType.Exposed.value, 0, false, Some(now))
        val model   = ExposureModel(0L, userId, videoId, now, now, 1, scene, Some(now), Some(now))
        (ensure, Some(event), Some(model))
      } else (ensure, None, None)
    }
    val (events, models) = writes.map(toMap).filter(_(0)).map(t => (t(1).get, t(2).get)).unzip
    try
      db.executeTransaction {
        db.insertBatch(events)
        db.execute(saveExposuresSql(models))
        import sqala.static.dsl.*
        val dsl = from(ExposureModel).filter(e => e.userId.in(userIds) && e.videoId.in(videoIds))
        Right(db.fetch(query(dsl)).map(_.restoreExposure))
      }
    catch
      case e: Exception =>
        error(e.getMessage)
        Left(e)
  }
}

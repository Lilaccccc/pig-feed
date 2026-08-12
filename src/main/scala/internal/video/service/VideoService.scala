package internal.video.service

import internal.infra.rabbitmq.RabbitMQ
import internal.video.entity.VideoPublishedEvent.toEvent
import internal.video.entity.{Video, VideoCreateResult, VideoPublishedEvent}
import internal.infra.enums.*
import internal.video.enums.VideoStatus.Deleted
import internal.infra.errors.*
import internal.video.repository.VideoRepository
import utils.base.ColoredLogger

object VideoService extends ColoredLogger {
  // 创建已发布视频；Idempotency-Key 命中时返回已有视频。
  def createPublished(authorId: Long, title: String, description: Option[String], mediaUrl: String, coverUrl: String, idempotencyKey: String): Either[Exception, VideoCreateResult] = {
    if idempotencyKey.trim.length > MaxIdempotencyKeyLength then return Left(ErrIdempotencyKeyTooLong())
    val save = (video: Video) =>
      VideoRepository
        .save(video)
        .map(v => {
          v.toEvent.foreach(RabbitMQ.publish)
          VideoCreateResult(v, true)
        })

    // 客户端重试同一次创建请求时，先通过作者和幂等键找回原视频。
    VideoRepository.findByAuthorAndIdempotencyKey(authorId, Some(idempotencyKey.trim)) match {
      case Left(_) =>
        Video(authorId, title, description.getOrElse(""), mediaUrl, coverUrl, idempotencyKey)
          .flatMap(save)
          .fold(
            err => Left(err.asInstanceOf[Exception]),
            video => Right(video)
          )
      case Right(value) => Right(VideoCreateResult(value, false))
    }
  }

  // 只返回已发布视频，删除或下线的视频在公开详情里表现为找不到。
  def get(videoId: Long): Either[Exception, Video] = {
    if videoId <= 0 then return Left(ErrInvalidVideoID().asInstanceOf[Exception])
    VideoRepository.findById(videoId)
  }

  // 查询某个作者公开发布的视频列表，使用 offset 分页。
  def listByAuthor(authorId: Long, limit: Int, offset: Int): Either[Exception, List[Video]] = {
    if authorId <= 0 then return Left(ErrInvalidAuthorID())
    if limit <= 0 then return Left(ErrInvalidLimit())
    if offset < 0 then return Left(ErrInvalidOffset())

    // 后端限制最大页大小，避免一次请求拉取过多数据。
    Right(VideoRepository.listByAuthor(authorId, if limit > 100 then 100 else limit, offset))
  }

  // 执行视频软删除，只有作者本人可以删除自己的视频。
  def delete(authorId: Long, videoId: Long): Either[Exception, Unit] = {
    if authorId <= 0 then return Left(ErrInvalidAuthorID())
    if videoId <= 0 then return Left(ErrInvalidVideoID())

    val delete = (entity: Video) =>
      entity
        .deleteBy(authorId)
        .flatMap(video => VideoRepository.updateStatus(video))
        .fold(err => Left(err.asInstanceOf[Exception]), _ => Right(()))

    VideoRepository
      .findByIdAnyStatus(videoId)
      .fold(
        err => Left(err.asInstanceOf[Exception]),
        // 软删除接口保持幂等：已经删除的视频再次删除仍然返回成功。
        video => if video.status == Deleted.value then Right(()) else delete(video)
      )
  }
}

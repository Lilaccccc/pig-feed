package internal.video.repository

import internal.infra.errors.*
import internal.video.entity.Video
import internal.video.enums.VideoStatus.Published
import internal.video.sql.*
import sqala.jdbc.JdbcTransactionContext
import sqala.metadata.*
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}

// 映射 video 表，保存视频主体信息和发布状态。
@table("video")
final case class VideoModel(
  @autoInc id: Long,
  authorId: Long,
  title: String,
  description: String,
  mediaUrl: String,
  coverUrl: String,
  status: Int,
  publishedAt: LocalDateTime,
  // IdempotencyKey 与 AuthorID 组成唯一索引，用于发布接口的安全重试。
  idempotencyKey: Option[String],
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime]
)

object VideoRepository extends ColoredLogger {
  // 确保每个视频都有一条统计记录
  def ensureStats(using JdbcTransactionContext) = db.execute(ensureStatsSql)

  // 在同一事务内写入视频记录和初始统计记录。
  def save(video: Video): Either[ErrDuplicateIdempotencyKey, Video] = {
    val model = VideoModel(
      id = 0,
      authorId = video.authorId,
      title = video.title,
      description = video.description,
      mediaUrl = video.mediaUrl,
      coverUrl = video.coverUrl,
      status = video.status,
      publishedAt = video.publishedAt,
      // 将空幂等键存为 NULL，配合唯一索引允许普通创建多次执行。
      idempotencyKey = if video.idempotencyKey.isBlank then None else Some(video.idempotencyKey),
      createdAt = Some(LocalDateTime.now),
      updatedAt = Some(LocalDateTime.now)
    )

    // video_stat 独立存储计数，便于互动接口只更新统计表。
    val stat = (videoId: Long) =>
      VideoStatModel(
        videoId = videoId,
        likeCount = video.likeCount.getOrElse(0L),
        commentCount = video.commentCount.getOrElse(0L),
        favoriteCount = video.favoriteCount.getOrElse(0L),
        createdAt = Some(LocalDateTime.now),
        updatedAt = Some(LocalDateTime.now)
      )

    Try(db.executeTransaction {
      val saveModel = db.insertAndReturn(model)
      db.insert(stat(saveModel.id))
      saveModel
    }) match {
      case Failure(exception) =>
        error(exception.getMessage)
        Left(ErrDuplicateIdempotencyKey())
      case Success(value) => Right(video.copy(id = Some(value.id), createdAt = value.createdAt, updatedAt = value.updatedAt))
    }
  }

  // 查询公开可见的视频详情，只返回 Published 状态。
  def findById(id: Long): Either[ErrVideoNotFound, Video] = {
    db.fetchTo[Video](findByVideoSql(id, Some(Published)))
      .headOption
      .map(video => Right(video))
      .getOrElse(Left(ErrVideoNotFound()))
  }

  def findExistsMapById(videoIds: List[Long]): Map[Long, Boolean] = {
    import sqala.static.dsl.*
    val dsl = from(VideoModel).filter(v => v.id.in(videoIds) && v.status == Published.value).select(_.id)
    db.fetch(query(dsl)).map(id => (id, true)).toMap
  }

  // 查询任意状态视频，供作者删除等内部流程使用。
  def findByIdAnyStatus(id: Long): Either[ErrVideoNotFound, Video] = {
    db.fetchTo[Video](findByVideoSql(id, None))
      .headOption
      .map(video => Right(video))
      .getOrElse(Left(ErrVideoNotFound()))
  }

  // 根据作者和幂等键查找已创建视频
  def findByAuthorAndIdempotencyKey(authorId: Long, key: Option[String] = None): Either[ErrVideoNotFound, Video] = {
    if key.isEmpty || key.get.isBlank then return Left(ErrVideoNotFound())
    db.fetchTo[Video](findByAuthorAndIdempotencyKeySql(authorId, key.get))
      .headOption
      .map(video => Right(video))
      .getOrElse(Left(ErrVideoNotFound()))
  }

  // 按发布时间倒序返回作者已发布视频。
  def listByAuthor(authorId: Long, limit: Int, offset: Int) = {
    // 查询模型逐条恢复为领域对象，应用层无需知道数据库联表细节。
    db.fetchTo[Video](findListByAuthorSql(authorId, Published, limit, offset))
  }

  // 只更新状态字段，用于软删除。
  def updateStatus(video: Video): Either[ErrVideoNotFound, Video] = {
    val model = video.modelVideo
    if model.id <= 0 then return Left(ErrVideoNotFound())
    val now = Some(LocalDateTime.now)
    if db.update(model.copy(updatedAt = now), true) > 0 then Right(video.copy(updatedAt = now))
    else Left(ErrVideoNotFound())
  }

  // 校验并锁定已发布视频，互动写入前都会经过这里。
  def lockPublishedVideo(videoId: Long)(using JdbcTransactionContext): VideoModel = {
    import sqala.static.dsl.*
    val dsl = from(VideoModel).filter(v => v.id == videoId && v.status == Published.value)
    val sql = query(dsl.forUpdate)
    db.fetch(sql).headOption.getOrElse(throw ErrVideoNotFound())
  }
}

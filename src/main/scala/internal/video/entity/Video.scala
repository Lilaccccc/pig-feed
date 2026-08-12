package internal.video.entity

import internal.infra.enums.*
import internal.video.enums.MaxDescriptionLength
import internal.video.enums.VideoStatus.*
import internal.infra.errors.*
import internal.video.repository.VideoModel
import utils.result.ErrorResult

import java.time.LocalDateTime

// 视频聚合根，包含内容信息、发布状态和统计快照。
case class Video(
  id: Option[Long] = None,
  authorId: Long,
  title: String,
  description: String,
  mediaUrl: String,
  coverUrl: String,
  status: Int,
  likeCount: Option[Long] = None,
  commentCount: Option[Long] = None,
  favoriteCount: Option[Long] = None,
  publishedAt: LocalDateTime,
  idempotencyKey: String,
  createdAt: Option[LocalDateTime] = None,
  updatedAt: Option[LocalDateTime] = None,
)

object Video {
  // 创建一个直接发布的视频，适合当前项目的发布流程。
  def apply(authorId: Long, title: String, description: String, mediaUrl: String, coverUrl: String, idempotencyKey: String): Either[ErrorResult, Video] = {
    // 新建视频直接进入 Published 状态，同时记录发布时间用于 Feed 排序。
    val news = () =>
      new Video(
        authorId = authorId,
        title = title.trim,
        description = description.trim,
        mediaUrl = mediaUrl.trim,
        coverUrl = coverUrl.trim,
        status = Published.value,
        publishedAt = LocalDateTime.now,
        idempotencyKey = idempotencyKey
      )

    Either
      .cond(authorId > 0, title, ErrInvalidAuthorID())
      .flatMap(t => Either.cond(!title.isBlank, title, ErrEmptyTitle()))
      .flatMap(t => Either.cond(t.length <= MaxTitleLength, description, ErrTitleTooLong()))
      .flatMap(d => Either.cond(d.length <= MaxDescriptionLength, mediaUrl, ErrDescriptionTooLong()))
      .flatMap(m => Either.cond(!m.isBlank, coverUrl, ErrEmptyMediaURL()))
      .flatMap(c => Either.cond(!c.isBlank, idempotencyKey, ErrEmptyCoverURL()))
      .flatMap(i => Either.cond(i.length <= MaxIdempotencyKeyLength, news(), ErrIdempotencyKeyTooLong()))
  }

  extension (video: Video) {
    // 执行作者权限校验并把视频置为删除状态。
    def deleteBy(authorId: Long): Either[ErrorResult, Video] = {
      Either
        .cond(authorId > 0, video.authorId, ErrInvalidAuthorID())
        .flatMap(aId => Either.cond(aId == authorId, video.status, ErrVideoPermissionDenied()))
        // 删除采用软删除，保留原始记录用于审计、统计或后续恢复。
        .flatMap(s => Either.cond(s != Deleted.value, video.copy(status = Deleted.value), ErrVideoDeleted()))
    }

    def modelVideo = VideoModel(
      id = video.id.getOrElse(0L),
      authorId = video.authorId,
      title = video.title,
      description = video.description,
      mediaUrl = video.mediaUrl,
      coverUrl = video.coverUrl,
      status = video.status,
      publishedAt = video.publishedAt,
      idempotencyKey = Some(video.idempotencyKey),
      createdAt = None,
      updatedAt = None
    )
  }
}

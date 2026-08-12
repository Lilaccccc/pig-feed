package internal.video.controller.dto

import internal.video.entity.Video
import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

// 视频详情响应，包含视频主体字段和互动计数。
case class VideoResponse(
  id: Long,
  authorId: Long,
  title: String,
  description: String,
  mediaUrl: String,
  coverUrl: String,
  status: Int,
  likeCount: Long,
  commentCount: Long,
  favoriteCount: Long,
  publishedAt: LocalDateTime,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime
) derives Codec, Schema

object VideoResponse {
  extension (v: Video) {
    def videoResponseFromDomain = VideoResponse(
      v.id.getOrElse(0L),
      v.authorId,
      v.title,
      v.description,
      v.mediaUrl,
      v.coverUrl,
      v.status,
      v.likeCount.getOrElse(0L),
      v.commentCount.getOrElse(0L),
      v.favoriteCount.getOrElse(0L),
      v.publishedAt,
      v.createdAt.orNull,
      v.updatedAt.orNull
    )
  }
}

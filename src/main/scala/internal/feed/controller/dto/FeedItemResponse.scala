package internal.feed.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

// Feed 中单条视频卡片的响应结构。
final case class FeedItemResponse(
  videoId: Long,
  authorId: Long,
  authorNickname: String,
  authorAvatarUrl: String,
  title: String,
  description: String,
  mediaUrl: String,
  coverUrl: String,
  likeCount: Long,
  commentCount: Long,
  favoriteCount: Long,
  liked: Boolean,
  favorited: Boolean,
  following: Boolean,
  publishedAt: LocalDateTime
) derives Codec, Schema

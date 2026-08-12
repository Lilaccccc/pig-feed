package internal.feed.entity

import io.circe.Codec

import java.time.LocalDateTime

// 保存视频卡片中相对稳定的展示字段。
final case class FeedCard(
  videoId: Long,
  authorId: Long,
  authorNickname: String,
  authorAvatarUrl: String,
  title: String,
  description: String,
  mediaUrl: String,
  coverUrl: String,
  publishedAt: LocalDateTime
) derives Codec

object FeedCard {
  def empty(videoId: Long = 0L): FeedCard = FeedCard(0L, 0L, "", "", "", "", "", "", LocalDateTime.now)
}

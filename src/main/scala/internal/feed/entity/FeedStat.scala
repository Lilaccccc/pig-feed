package internal.feed.entity

import io.circe.Codec

// 保存视频卡片中的高频计数字段。
final case class FeedStat(
  videoId: Long,
  likeCount: Long,
  commentCount: Long,
  favoriteCount: Long
) derives Codec

object FeedStat {
  def empty(videoId: Long = 0L): FeedStat = FeedStat(videoId, 0L, 0L, 0L)
}

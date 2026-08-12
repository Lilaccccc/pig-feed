package internal.feed.entity

import io.circe.Codec

import java.time.LocalDateTime

// Feed 页缓存中的轻量条目，只保存排序和组装所需字段。
final case class FeedPageItem(
  videoId: Long,
  authorId: Option[Long] = None,
  publishedAt: Option[LocalDateTime] = None,
  hotScore: Option[Int] = None
) derives Codec
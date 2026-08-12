package internal.feed.entity

import internal.feed.enums.Scene
import io.circe.Codec

// 页缓存中的轻量结果，卡片和计数会在组装阶段批量读取。
final case class FeedPage(
  scene: Scene,
  items: List[FeedPageItem],
  nextCursor: String,
  hasMore: Boolean
) derives Codec

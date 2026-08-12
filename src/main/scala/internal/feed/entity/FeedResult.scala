package internal.feed.entity

import internal.feed.enums.Scene

// 游标分页结果，NextCursor 供客户端请求下一页。
final case class FeedResult(
  scene: Scene,
  items: List[FeedItem],
  nextCursor: String,
  hasMore: Boolean
)

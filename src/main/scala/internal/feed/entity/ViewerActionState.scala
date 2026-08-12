package internal.feed.entity

// 保存当前用户对一批视频的互动状态。
final case class ViewerActionState(
  videoId: Long,
  liked: Option[Boolean] = None,
  favorited: Option[Boolean] = None
)

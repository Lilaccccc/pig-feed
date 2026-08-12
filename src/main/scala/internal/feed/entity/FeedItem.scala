package internal.feed.entity

import internal.interaction.enums.HotScoreWeight

import java.time.LocalDateTime

// 是 Feed 页面需要展示的一条视频卡片数据。
final case class FeedItem(
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
  liked: Option[Boolean] = None,
  favorited: Option[Boolean] = None,
  following: Option[Boolean] = None,
  hotScore: Long,
  publishedAt: LocalDateTime
)

object FeedItem {
  // 从查询结果恢复 FeedItem，并清洗展示用字符串。
  def restoreFeedItem(
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
    liked: Option[Boolean],
    favorited: Option[Boolean],
    following: Option[Boolean],
    hotScore: Long,
    publishedAt: LocalDateTime
  ) = FeedItem(
    videoId,
    authorId,
    authorNickname.trim,
    authorAvatarUrl.trim,
    title.trim,
    description.trim,
    mediaUrl.trim,
    coverUrl.trim,
    likeCount,
    commentCount,
    favoriteCount,
    liked,
    favorited,
    following,
    scoreHotFeedItem(likeCount, commentCount, favoriteCount),
    publishedAt
  )

  // 计算热榜排序分：评论权重最高，收藏次之，点赞提供基础热度。
  private def scoreHotFeedItem(likeCount: Long, commentCount: Long, favoriteCount: Long) = {
    likeCount * HotScoreWeight.Like.value
      + commentCount * HotScoreWeight.Comment.value
      + favoriteCount * HotScoreWeight.Favorite.value
  }
}

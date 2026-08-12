package internal.relation.entity

import java.time.LocalDateTime

// 关注列表或粉丝列表中的用户展示数据。
final case class UserItem(
  userId: Long,
  nickname: String,
  avatarUrl: String,
  bio: String,
  followedAt: LocalDateTime
)

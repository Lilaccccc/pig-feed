package internal.relation.entity

import java.time.LocalDateTime

// 保存用户维度的关注数和粉丝数。
final case class RelationStat(
  userId: Long,
  followingCount: Int,
  followerCount: Int,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime
)

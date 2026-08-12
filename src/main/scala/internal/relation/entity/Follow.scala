package internal.relation.entity

import internal.relation.enums.FollowStatus

import java.time.LocalDateTime

// 表示一个用户对另一个用户的关注关系，取关使用软状态保留历史。
final case class Follow(
  id: Long,
  userId: Long,
  targetUserId: Long,
  status: Int,
  idempotencyKey: String,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime
) {
  def active: Boolean = status == FollowStatus.Active.value
}

package internal.interaction.entity

import internal.interaction.enums.ActionStatus.Active

import java.time.LocalDateTime

// 表示用户对视频的一类互动状态，例如点赞或收藏。
final case class Action(
  id: Long,
  userId: Long,
  videoId: Long,
  actionType: String,
  status: Int,
  idempotencyKey: String,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime
) {
  // 判断点赞或收藏当前是否处于有效状态。
  def active: Boolean = this.status == Active.value
}

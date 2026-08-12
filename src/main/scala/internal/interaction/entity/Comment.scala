package internal.interaction.entity

import internal.interaction.enums.CommentStatus
import internal.interaction.enums.CommentStatus.Deleted
import internal.interaction.repository.CommentModel

import java.time.LocalDateTime

// 表示视频评论，包含评论者展示信息和软删除状态。
final case class Comment(
  id: Long,
  videoId: Long,
  userId: Long,
  userNickname: String,
  userAvatarUrl: String,
  content: String,
  status: Int,
  idempotencyKey: String,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime
) {
  // 判断评论是否已经被软删除。
  def deleted: Boolean = this.status == Deleted.value

  def toModel: CommentModel = CommentModel(
    id,
    videoId,
    userId,
    content,
    status,
    idempotencyKey.trim,
    Some(createdAt),
    Some(updatedAt)
  )
}

object Comment {
  def toCreateComment(videoId: Long, userId: Long, content: String, status: CommentStatus, idempotencyKey: String) = Comment(
    0,
    videoId,
    userId,
    null,
    null,
    content,
    status.value,
    idempotencyKey,
    null,
    null
  )
}

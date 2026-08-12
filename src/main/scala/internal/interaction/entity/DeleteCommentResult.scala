package internal.interaction.entity

final case class DeleteCommentResult(
  commentId: Long,
  status: Int,
  commentCount: Long
)

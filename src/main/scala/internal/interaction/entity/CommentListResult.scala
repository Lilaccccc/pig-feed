package internal.interaction.entity

final case class CommentListResult(
  items: List[Comment],
  nextCursor: String,
  hasMore: Boolean
)

package internal.message.entity

final case class ListResult(
  items: List[Message],
  nextCursor: String,
  hasMore: Boolean
)
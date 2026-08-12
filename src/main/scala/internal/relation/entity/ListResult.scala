package internal.relation.entity

// 关注列表或粉丝列表的游标分页结果。
final case class ListResult(
  items: List[UserItem],
  nextCursor: String,
  hasMore: Boolean
)

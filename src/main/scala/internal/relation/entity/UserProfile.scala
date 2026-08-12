package internal.relation.entity

// 关系通知里展示触发用户所需的资料。
final case class UserProfile(
  userId: Long,
  nickname: String,
  avatarUrl: String,
  bio: String
)

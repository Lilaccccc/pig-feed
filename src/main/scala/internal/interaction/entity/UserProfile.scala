package internal.interaction.entity

// 保存互动消息需要展示的用户资料。
final case class UserProfile(
  id: Long,
  nickname: String,
  avatarUrl: String
)

package internal.interaction.entity

final case class ActionStateResult(
  videoId: Long,
  actionType: String,
  active: Boolean,
  likeCount: Long,
  commentCount: Long,
  favoriteCount: Long,
  delta: Int,
  idempotencyKey: String
)

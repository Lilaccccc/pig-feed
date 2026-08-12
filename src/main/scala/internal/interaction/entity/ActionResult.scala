package internal.interaction.entity

final case class ActionResult(
  videoId: Long,
  actionType: String,
  active: Boolean,
  likeCount: Long,
  favoriteCount: Long
)

package internal.interaction.enums

enum CommentCount(val value: String) {
  case Like extends CommentCount("like_count")
  case Favorite extends CommentCount("favorite_count")
  case Comment extends CommentCount("comment_count")
}
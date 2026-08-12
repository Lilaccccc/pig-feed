package internal.interaction.enums

enum CommentStatus(val value: Int) {
  case Normal  extends CommentStatus(1)
  case Deleted extends CommentStatus(2)
}

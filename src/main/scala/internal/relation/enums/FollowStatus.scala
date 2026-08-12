package internal.relation.enums

enum FollowStatus(val value: Int) {
  case Active   extends FollowStatus(1)
  case Canceled extends FollowStatus(2)

  case Unknown extends FollowStatus(0)
}

object FollowStatus {
  def value(status: Boolean): FollowStatus = if (status) Active else Canceled
}

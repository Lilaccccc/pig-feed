package internal.message.enums

enum MessageType(val value: String) {
  case Like    extends MessageType("LIKE")
  case Comment extends MessageType("COMMENT")
  case Follow  extends MessageType("FOLLOW")
  case System  extends MessageType("SYSTEM")
}

object MessageType {
  def value(str: String): MessageType = {
    val trim = str.trim
    MessageType.values.find(_.value.equals(trim)).getOrElse(System)
  }
}

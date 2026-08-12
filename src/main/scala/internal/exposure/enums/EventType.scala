package internal.exposure.enums

enum EventType(val value: String) {
  case Exposed  extends EventType("exposed")
  case Play     extends EventType("play")
  case Complete extends EventType("complete")
  case Skip     extends EventType("skip")
  case Unknown  extends EventType("unknown")
}

object EventType {
  def value(eventType: String): EventType = {
    val eventTypeVal = eventType.trim.toLowerCase
    EventType.values.find(_.value.equals(eventTypeVal)).getOrElse(Unknown)
  }
}

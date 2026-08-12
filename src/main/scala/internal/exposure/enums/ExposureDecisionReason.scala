package internal.exposure.enums

enum ExposureDecisionReason(val value: String) {
  case Fresh   extends ExposureDecisionReason("fresh")
  case RecentlyExposed extends ExposureDecisionReason("recently_exposed")
  case Unknown extends ExposureDecisionReason("unknown")
}

object ExposureDecisionReason {
  def value(reason: String): ExposureDecisionReason = {
    val reasonTrim = reason.trim.toLowerCase
    ExposureDecisionReason.values.find(_.equals(reasonTrim)).getOrElse(Unknown)
  }
}

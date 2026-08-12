package internal.interaction.enums

enum ActionType(val value: String, val cacheKey: String) {
  case Like extends ActionType("LIKE", "like_count")
  case Favorite extends ActionType("FAVORITE", "favorite_count")
  case Unknown extends ActionType("UNKNOWN", "unknown")
}

object ActionType {
  def value(str: String): ActionType = {
    val trim = str.trim.toUpperCase
    ActionType.values.find(_.value.equals(trim)).getOrElse(Unknown)
  }
}
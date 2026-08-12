package internal.interaction.enums

enum HotScoreWeight(val value: Int) {
  case Like extends HotScoreWeight(3)
  case Favorite extends HotScoreWeight(4)
  case Comment extends HotScoreWeight(5)
}

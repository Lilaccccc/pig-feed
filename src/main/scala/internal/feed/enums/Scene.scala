package internal.feed.enums

import io.circe.Codec

// 表示不同 Feed 场景，应用层通过场景选择对应策略。
enum Scene(val value: String) derives Codec {
  case Timeline  extends Scene("timeline")
  case Recommend extends Scene("recommend")
  case Following extends Scene("following")
  case Hot       extends Scene("hot")
  case Default   extends Scene("timeline")
}

object Scene {
  def value(str: String): Scene = {
    val trim = str.trim.toLowerCase
    Scene.values.find(_.value.equals(trim)).getOrElse(Default)
  }
}

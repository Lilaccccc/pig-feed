package internal.auth.enums.account

import io.circe.Codec

enum Role(val name: String) derives Codec {
  case Admin   extends Role("admin")
  case User    extends Role("user")
  case Unknown extends Role("unknown")
}

object Role {
  def fromName(name: String) = Role.values.find(_.name.equals(name)).getOrElse(Unknown)
}

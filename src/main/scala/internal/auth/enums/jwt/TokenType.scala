package internal.auth.enums.jwt

import io.circe.Codec
import utils.base.config.enums.JwtConfig

enum TokenType(val name: String, val ttl: Int) derives Codec {
  case Access extends TokenType("Access", JwtConfig.accessTtl)
}
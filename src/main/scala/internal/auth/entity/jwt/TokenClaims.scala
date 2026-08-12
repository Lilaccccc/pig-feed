package internal.auth.entity.jwt

import io.circe.Codec
import internal.auth.enums.account.Role
import internal.auth.enums.jwt.TokenType

// 真正写入 JWT 的声明，嵌入 RegisteredClaims 获得 exp、iat、jti 等标准字段。
final case class TokenClaims(
  userId: Long,
  role: Role,
  tokenType: TokenType
) derives Codec

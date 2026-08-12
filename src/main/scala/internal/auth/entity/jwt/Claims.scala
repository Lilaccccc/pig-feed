package internal.auth.entity.jwt

import internal.auth.enums.account.Role
import internal.auth.enums.jwt.TokenType
import internal.infra.errors.*
import io.circe.Codec
import pdi.jwt.JwtClaim

// 业务侧读取到的 token 信息，避免 HTTP 层直接依赖第三方 JWT 结构。
final case class Claims(
  userId: Long,
  role: Role,
  tokenType: TokenType,
  jwtId: String,
  issuedAt: Long,
  expiresAt: Long
) derives Codec

object Claims {
  def claim = (jwtClaim: JwtClaim, tokenClaim: TokenClaims) =>
    Claims(
      userId = tokenClaim.userId,
      role = tokenClaim.role,
      tokenType = tokenClaim.tokenType,
      jwtId = jwtClaim.jwtId.getOrElse(""),
      issuedAt = jwtClaim.issuedAt.getOrElse(0),
      expiresAt = jwtClaim.expiration.getOrElse(0)
    )

  def cond = (claims: Claims, expectedType: TokenType) =>
    Either
      .cond(!claims.jwtId.isBlank, claims, ErrParseJWTToken())
      .flatMap(c => Either.cond(c.tokenType == expectedType, c, ErrInvalidTokenType()))
      .flatMap(c => Either.cond(c.userId > 0, c, ErrInvalidTokenUserID()))
}

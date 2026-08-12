package internal.auth.entity.jwt

import internal.auth.enums.account.Role
import internal.auth.enums.jwt.TokenType
import internal.infra.errors.*
import pdi.jwt.*
import utils.base.config.enums.JwtConfig
import utils.base.idgenerator.IdGenerator
import utils.base.{ColoredLogger, decode, json}
import utils.result.ErrorResult

import java.time.Clock

object JwtManager extends ColoredLogger {
  // 签发访问 token，当前系统使用 HS256 对称签名。
  def signAccessToken(userId: Long, role: Role): Either[ErrorResult, String] = {
    if userId <= 0 then return Left(ErrInvalidUserID())

    // jti 是 token 唯一标识，后续可以扩展为黑名单或审计日志依据。
    val jti    = IdGenerator.hex16Id(16)
    val claims = JwtClaim(
      TokenClaims(userId, role, TokenType.Access).json
    ).withId(jti).expiresIn(TokenType.Access.ttl).issuedNow.startsNow

    Right(Jwt.encode(claims, JwtConfig.secret, JwtAlgorithm.HS256))
  }

  // 解析 token，并校验签名算法、过期时间和 token 类型。
  def parseAndValidateToken(token: String, expectedType: TokenType): Either[Exception & ErrorResult, Claims] = {
    if token.isBlank then return Left(ErrEmptyToken())

    Jwt
      // 限定签名算法可以避免算法降级类攻击。
      .decode(token, JwtConfig.secret, Seq(JwtAlgorithm.HS256), JwtOptions.DEFAULT)
      .toOption
      .map(jwtClaim =>
        jwtClaim.content
          .decode[TokenClaims]
          // 用 Either.cond 统一左类型为 ErrorResult
          .map(tokenClaim => Claims.cond(Claims.claim(jwtClaim, tokenClaim), expectedType))
          .getOrElse(Left(ErrParseJWTToken()))
      )
      .getOrElse(Left(ErrParseJWTToken()))
  }

  private given Clock = Clock.systemDefaultZone
}

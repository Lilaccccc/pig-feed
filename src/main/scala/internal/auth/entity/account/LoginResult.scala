package internal.auth.entity.account

import internal.auth.enums.jwt.TokenType
import io.circe.Codec
import sttp.tapir.Schema

// 登录成功后返回给 HTTP 层的 token 数据。
final case class LoginResult(
  accessToken: String,
  tokenType: String,
  expiresInSeconds: Long = TokenType.Access.ttl
) derives Codec, Schema

object LoginResult {
  def bearer(token: String) = LoginResult(
    accessToken = token,
    tokenType = "Bearer"
  )
}

package internal.auth.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 账号登录请求
final case class LoginByPasswordRequest(
  account: String,
  password: String
) derives Codec, Schema

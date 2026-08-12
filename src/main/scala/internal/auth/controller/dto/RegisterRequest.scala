package internal.auth.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 账号注册请求
final case class RegisterRequest(
  account: String,
  password: String,
  nickname: String
) derives Codec, Schema

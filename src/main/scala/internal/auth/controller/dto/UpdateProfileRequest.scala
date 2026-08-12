package internal.auth.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 用户资料更新请求
final case class UpdateProfileRequest(
  nickname: Option[String] = None,
  avatarUrl: Option[String] = None,
  bio: Option[String] = None
) derives Codec, Schema

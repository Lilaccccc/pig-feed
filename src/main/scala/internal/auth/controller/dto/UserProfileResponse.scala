package internal.auth.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 用户信息响应
final case class UserProfileResponse(
  id: Long,
  account: String,
  nickname: String,
  avatarUrl: String,
  bio: String,
  status: Int,
  role: String,
  followingCount: Int,
  followerCount: Int,
  workCount: Int
) derives Codec, Schema

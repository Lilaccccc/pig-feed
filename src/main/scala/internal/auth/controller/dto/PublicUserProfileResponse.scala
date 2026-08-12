package internal.auth.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 公开用户信息响应，隐藏账号、角色和状态等内部字段。
final case class PublicUserProfileResponse(
  id: Long,
  nickname: String,
  avatarUrl: String,
  bio: String,
  followingCount: Int,
  followerCount: Int,
  workCount: Int
) derives Codec, Schema
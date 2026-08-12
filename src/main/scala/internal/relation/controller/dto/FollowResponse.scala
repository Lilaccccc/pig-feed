package internal.relation.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 关注或取关后的关系状态响应
final case class FollowResponse(
  userId: Long,
  targetUserId: Long,
  status: Int,
  following: Boolean,
  followingCount: Int,
  followerCount: Int
) derives Codec, Schema

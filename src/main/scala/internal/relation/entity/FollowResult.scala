package internal.relation.entity

import internal.relation.controller.dto.FollowResponse

// 关注或取关后的关系状态和计数。
final case class FollowResult(
  userId: Long,
  targetUserId: Long,
  status: Int,
  following: Boolean,
  followingCount: Int,
  followerCount: Int
) {
  def followResponse: FollowResponse = FollowResponse(
    userId,
    targetUserId,
    status,
    following,
    followingCount,
    followerCount
  )
}

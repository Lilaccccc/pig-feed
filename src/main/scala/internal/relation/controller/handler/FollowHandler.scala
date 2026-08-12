package internal.relation.controller.handler

import internal.relation.controller.dto.*
import internal.relation.controller.dto.RelationListResponse.relationListResponseFromResult
import internal.relation.service.FollowService
import utils.base.ColoredLogger
import utils.result.throws

object FollowHandler extends ColoredLogger {
  // 关注 取消关注
  def follow(userId: Long, targetUserId: Long, active: Boolean, idempotencyKey: String): FollowResponse = FollowService
    .follow(userId, targetUserId, active, idempotencyKey)
    .fold(_.throws(using this), _.followResponse)

  // 查询当前用户关注列表
  def listFollowing(userId: Long, cursor: String, limit: Int): RelationListResponse = FollowService
    .listFollowing(userId, cursor.trim, limit)
    .fold(_.throws(using this), relationListResponseFromResult)

  // 查询当前用户粉丝列表
  def listFollowers(userId: Long, cursor: String, limit: Int): RelationListResponse = FollowService
    .listFollowers(userId, cursor, limit)
    .fold(_.throws(using this), relationListResponseFromResult)
}

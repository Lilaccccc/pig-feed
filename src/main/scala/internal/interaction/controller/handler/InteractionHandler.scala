package internal.interaction.controller.handler

import internal.auth.enums.account.Role
import internal.infra.errors.*
import internal.interaction.Service.ActionService
import internal.interaction.controller.dto.*
import internal.interaction.controller.dto.ActionResponse.actionResponseFromResult
import internal.interaction.controller.dto.CommentListResponse.commentListResponseFromResult
import internal.interaction.controller.dto.CommentResponse.commentResponseFromDomain
import internal.interaction.enums
import internal.interaction.enums.ActionType
import utils.base.ColoredLogger
import utils.result.throws

object InteractionHandler extends ColoredLogger {
  // 点赞和取消点赞共用参数解析逻辑，active 决定最终状态。
  def likeOrFavorite(userId: Long, videoId: Long, idempotencyKey: String, active: Boolean, actionType: ActionType): ActionResponse = {
    val request = (userId = userId, videoId = videoId, idempotencyKey = idempotencyKey)
    val result  = actionType match {
      case enums.ActionType.Like     => if active then ActionService.like(request) else ActionService.unlike(request)
      case enums.ActionType.Favorite => if active then ActionService.favorite(request) else ActionService.unFavorite(request)
      case enums.ActionType.Unknown  => ErrActionType().throws(using this)
    }
    result.fold(err => err.throws(using this), actionResponseFromResult)
  }

  // 创建视频评论，videoId 来自路径，评论内容来自请求体。
  def createComment(userId: Long, videoId: Long, idempotencyKey: String, createCommentRequest: CreateCommentRequest): CommentResponse = try {
    debug(s"createCommentRequest::$createCommentRequest")
    ActionService
      .createComment(userId, videoId, createCommentRequest.content, idempotencyKey)
      .fold(err => err.throws(using this), result => result.comment.commentResponseFromDomain(Some(result.commentCount)))
  } catch {
    case e: Exception => error(e.getMessage); null
  }

  // 查询指定视频的评论列表，分页参数来自 query。
  def listComments(videoId: Long, limit: Int, cursor: String): CommentListResponse = ActionService
    .listComments(videoId, cursor, limit)
    .fold(err => err.throws(using this), commentListResponseFromResult)

  // 删除评论，权限判断交给应用层和仓储层完成
  def deleteComment(userId: Long, role: Role, commentId: Long): DeleteCommentResponse = ActionService
    .deleteComment(userId, commentId, role)
    .fold(err => err.throws(using this), result => DeleteCommentResponse(result.commentId, result.status, result.commentCount))
}

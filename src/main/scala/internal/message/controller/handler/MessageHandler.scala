package internal.message.controller.handler

import internal.message.controller.dto.*
import internal.message.enums.MessageType
import internal.message.service.MessageService
import utils.base.ColoredLogger
import utils.result.throws

object MessageHandler extends ColoredLogger {
  // 查询当前登录用户的消息列表。
  def list(userId: Long, cursor: String, limit: Int): MessageListResponse = MessageService
    .list(userId, cursor, limit)
    .fold(err => err.throws(using this), MessageListResponse.responseFromDomain)

  // 查询当前登录用户未读消息数。
  def countUnread(userId: Long): UnreadStatResponse = MessageService
    .countUnread(userId)
    .fold(err => err.throws(using this), result => UnreadStatResponse(result.unreadCount))

  // 将当前登录用户的指定消息标记为已读。
  def markRead(userId: Long, markReadRequest: MarkReadRequest): MarkReadResponse = MessageService
    .markRead(userId, markReadRequest.messageIds)
    .fold(err => err.throws(using this), result => MarkReadResponse(result.updatedCount))

  // 供内部事件链路生成用户消息。
  def create(idempotencyKey: String, createMessageRequest: CreateMessageRequest): MessageResponse = MessageService
    .createFromActorEvent(
      createMessageRequest.userId,
      MessageType.value(createMessageRequest.messageType),
      createMessageRequest.title,
      createMessageRequest.content,
      createMessageRequest.eventId,
      idempotencyKey,
      createMessageRequest.actorId,
      createMessageRequest.actorNickname,
      createMessageRequest.actorAvatarUrl
    )
    .fold(err => err.throws(using this), result => MessageResponse.responseFromDomain(result.message))
}

package internal.message.service

import internal.feed.utils.decodeCursor
import internal.message.entity.*
import internal.message.enums.*
import internal.infra.enums.*
import internal.infra.errors.*
import internal.message.repository.MessageRepository
import utils.base.ColoredLogger

object MessageService extends ColoredLogger {
  // 将内部事件转换成用户消息，eventID/idempotencyKey 命中时返回既有消息。
  def createFromEvent(userId: Long, messageType: MessageType, title: String, content: String, eventId: String, idempotencyKey: String): Either[Exception, CreateResult] = {
    createFromActorEvent(userId, messageType, title, content, eventId, idempotencyKey, 0, "", "")
  }

  // 将内部事件转换成带触发用户信息的消息。
  def createFromActorEvent(
    userId: Long,
    messageType: MessageType,
    title: String,
    content: String,
    eventId: String,
    idempotencyKey: String,
    actorId: Long,
    actorNickname: String,
    actorAvatarUrl: String
  ): Either[Exception, CreateResult] = {
    val idempotencyKeyTrim = idempotencyKey.trim
    if idempotencyKeyTrim.length > MaxIdempotencyKeyLength then return Left(ErrIdempotencyKeyTooLong())
    val result = (m: Message) => {
      val message = m.withActor(actorId, actorNickname, actorAvatarUrl)
      val save    = MessageRepository.create(message, idempotencyKeyTrim)
      save.message
        .map(created => Right(CreateResult(created, save.inserted)))
        .getOrElse(Left(ErrSaveMessageFailed()))
    }
    `new`(userId, messageType.value, title, content, eventId).fold(err => Left(err), result)
  }

  // 创建消息领域对象，负责接收人、类型、标题、内容和事件 ID 校验。
  private def `new`(userId: Long, messageType: String, title: String, content: String, eventId: String): Either[Exception, Message] = {
    if userId <= 0 then return Left(ErrInvalidUserID())
    val mType  = MessageType.value(messageType)
    val mTitle = title.trim
    if mTitle.isBlank then return Left(ErrEmptyTitle())
    if mTitle.length > MaxTitleLength then return Left(ErrTitleTooLong())
    val mContent = content.trim
    val mEventId = eventId.trim
    if mContent.isBlank then return Left(ErrEmptyContent())
    if mContent.length > MaxContentLength then return Left(ErrContentTooLong())
    if mEventId.length > MaxEventIDLength then return Left(ErrEventIDTooLong())
    val message = Message(
      None,
      userId,
      mType.value,
      mTitle,
      mContent,
      mEventId,
      None,
      None,
      None,
      false,
      None,
      None
    )
    Right(message)
  }

  extension (m: Message) {
    // 写入触发消息的用户展示信息。
    private def withActor(actorId: Long, nickname: String, avatarUrl: String) = {
      m.copy(actorId = if actorId > 0 then Some(actorId) else m.actorId, actorNickname = Some(nickname.trim), actorAvatarUrl = Some(avatarUrl.trim))
    }
  }

  // 查询当前用户消息列表，使用游标分页。
  def list(userId: Long, cursor: String, limit: Int): Either[Exception, ListResult] = {
    if userId <= 0L then return Left(ErrInvalidUserID())
    val normalizeLimit = limit.normalizeLimit

    val result = (parsedCursor: Option[MessageCursor]) => {
      val list       = MessageRepository.listByUser(userId, parsedCursor, normalizeLimit + 1)
      val hasMore    = list.size > normalizeLimit
      val items      = if hasMore then list.slice(0, normalizeLimit) else list
      val nextCursor = if list.nonEmpty then {
        val last = list.last
        MessageCursor(last.createdAt.orNull, last.id.getOrElse(0L)).encodeBase64
      } else ""
      Right(ListResult(items, nextCursor, hasMore))
    }

    parseMessageCursor(cursor).fold(err => Left(err), result)
  }

  // 将客户端传回的字符串游标解析成领域游标。
  private def parseMessageCursor(raw: String): Either[Exception, Option[MessageCursor]] = {
    if raw.isBlank then return Right(None)
    decodeCursor(raw)
      .flatMap(bytes => MessageCursor.decodeBytes(bytes))
      .fold(err => Left(err), result => Right(Some(result)))
  }

  // 查询当前用户未读数
  def countUnread(userId: Long): Either[Exception, UnreadStat] = {
    if userId <= 0L then return Left(ErrInvalidUserID())
    Right(UnreadStat(MessageRepository.countUnread(userId)))
  }

  // 将当前用户消息标记为已读，空 messageIDs 表示全部已读。
  def markRead(userId: Long, messageIds: List[Long]): Either[Exception, MarkReadResult] = {
    if userId <= 0L then return Left(ErrInvalidUserID())
    Right(MarkReadResult(MessageRepository.markRead(userId, messageIds.filter(_ > 0L))))
  }
}

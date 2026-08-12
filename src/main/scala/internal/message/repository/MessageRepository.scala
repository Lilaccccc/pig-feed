package internal.message.repository

import internal.message.entity.{Message, MessageCursor}
import internal.message.sql.listByUserSql
import sqala.jdbc.JdbcTransactionContext
import sqala.metadata.{autoInc, column, table}
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime
import scala.util.Try

@table("user_message")
final case class MessageModel(
  @autoInc id: Long,
  userId: Long,
  @column("type") messageType: String,
  title: String,
  content: String,
  // 保存触发消息的用户展示信息，消息列表可直接展示头像和昵称。
  actorId: Long,
  actorNickname: String,
  actorAvatarUrl: String,
  // 与 UserID 组成唯一索引，用于内部事件重复消费的幂等写入。
  eventId: String,
  // 与 UserID 组成唯一索引，用于内部接口重复请求的幂等写入。
  idempotencyKey: String,
  isRead: Boolean,
  createdAt: Option[LocalDateTime],
  readAt: Option[LocalDateTime]
)

object MessageRepository extends ColoredLogger {
  // 保存新消息；同一用户同一事件重复写入时返回既有消息。
  def create(message: Message, idempotencyKey: String): (message: Option[Message], inserted: Boolean) = {
    val idempotencyKeyTrim                     = idempotencyKey.trim
    val model                                  = message.toModel(idempotencyKeyTrim).copy(isRead = false, createdAt = Some(LocalDateTime.now), readAt = None)
    def result()(using JdbcTransactionContext) = findExisting(message.userId, message.eventId.trim, idempotencyKeyTrim)
      .map(existing => (Some(Message.conventByModel(existing)), false))
      .getOrElse((Some(Message.conventByModel(db.insertAndReturn(model))), true))
    Try(db.executeTransaction(result())).getOrElse((None, false))
  }

  private def findExisting(userId: Long, eventId: String, idempotencyKey: String)(using JdbcTransactionContext): Option[MessageModel] = {
    import sqala.static.dsl.*

    val dsl = if !eventId.isBlank && !idempotencyKey.isBlank then Some(from(MessageModel).filter(m => m.eventId.equals(eventId) && m.idempotencyKey.equals(idempotencyKey)))
    else if !eventId.isBlank then Some(from(MessageModel).filter(_.eventId.equals(eventId)))
    else if !idempotencyKey.isBlank then Some(from(MessageModel).filter(_.idempotencyKey.equals(idempotencyKey)))
    else None

    dsl.flatMap(sql => db.fetch(query(sql.filter(_.userId == userId).sortBy(_.id.desc).forUpdate)).headOption)
  }

  // 按创建时间倒序读取当前用户的消息列表。
  def listByUser(userId: Long, cursor: Option[MessageCursor], limit: Int): List[Message] = {
    import sqala.static.dsl.*
    val dsl = cursor.map(c => listByUserSql(userId, c, limit)).getOrElse(listByUserSql(userId, limit))
    val sql = query(dsl)
    db.fetch(sql).map(Message.conventByModel)
  }

  // 统计当前用户未读消息数。
  def countUnread(userId: Long): Long = {
    import sqala.static.dsl.*
    val dsl = from(MessageModel).filter(m => m.userId == userId && !m.isRead)
    val sql = query(dsl)
    db.fetchCount(sql)
  }

  // 将当前用户的指定消息标记为已读；空列表表示当前用户全部消息。
  def markRead(userId: Long, messageIds: List[Long]): Long = {
    import sqala.static.dsl.*
    val dsl = update[MessageModel]
      .set(_.readAt := LocalDateTime.now)
      .set(_.isRead := true)
      .where(m => m.userId == userId && !m.isRead)
    val sql = if messageIds.nonEmpty then dsl.where(_.id.in(messageIds)) else dsl
    db.execute(sql)
  }
}

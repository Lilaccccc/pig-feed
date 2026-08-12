package internal.message.sql

import internal.message.entity.MessageCursor
import internal.message.repository.MessageModel
import sqala.static.dsl.*

def listByUserSql(userId: Long, limit: Int) = from(MessageModel)
  .filter(_.userId == userId)
  .sortBy(_.createdAt.desc)
  .sortBy(_.id.desc)
  .limit(limit)

def listByUserSql(userId: Long, cursor: MessageCursor, limit: Int) = from(MessageModel)
  .filter(_.userId == userId)
  .filter(m => m.createdAt < cursor.createdAt || (m.createdAt == cursor.createdAt && m.id < cursor.messageId))
  .sortBy(_.createdAt.desc)
  .sortBy(_.id.desc)
  .limit(limit)

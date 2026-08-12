package internal.message.entity

import internal.message.repository.MessageModel

import java.time.LocalDateTime

final case class Message(
  id: Option[Long],
  userId: Long,
  messageType: String,
  title: String,
  content: String,
  eventId: String,
  actorId: Option[Long],
  actorNickname: Option[String],
  actorAvatarUrl: Option[String],
  isRead: Boolean,
  createdAt: Option[LocalDateTime],
  readAt: Option[LocalDateTime]
) {
  def toModel(idempotencyKey: String) = MessageModel(
    id.getOrElse(0L),
    userId,
    messageType,
    title,
    content,
    actorId.getOrElse(0L),
    actorNickname.getOrElse(""),
    actorAvatarUrl.getOrElse(""),
    optionalString(eventId),
    optionalString(idempotencyKey),
    isRead,
    None,
    None
  )

  private def optionalString(value: String) = Option(value.trim).filter(!_.isBlank).getOrElse("")
}

object Message {
  def conventByModel(model: MessageModel) = Message(
    Option(model.id),
    model.userId,
    model.messageType,
    model.title,
    model.content,
    model.eventId,
    Option(model.actorId),
    Option(model.actorNickname),
    Option(model.actorAvatarUrl),
    model.isRead,
    model.createdAt,
    model.readAt
  )
}

package internal.message.entity

import internal.infra.errors.*
import io.circe.Codec
import utils.base.{decode, json}

import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.util.Base64

// 保存消息列表分页的排序字段。
final case class MessageCursor(
  createdAt: LocalDateTime,
  messageId: Long
) derives Codec

object MessageCursor {
  def decodeBytes(bytes: Array[Byte]): Either[ErrInvalidCursor, MessageCursor] = {
    String(bytes, UTF_8).decode[MessageCursor] match {
      case Right(payload) => Right(payload)
      case Left(error)    => Left(ErrInvalidCursor())
    }
  }

  extension (m: MessageCursor) {
    def encodeBase64: String = Base64.getUrlEncoder.encodeToString(m.json.getBytes(UTF_8))
  }
}

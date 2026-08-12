package internal.interaction.entity

import internal.infra.errors.*
import io.circe.Codec
import utils.base.{decode, json}

import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.util.Base64

// 保存评论列表分页的排序字段。
final case class CommentCursor(
  createdAt: LocalDateTime,
  commentId: Long
) derives Codec

object CommentCursor {
  def decodeBytes(bytes: Array[Byte]): Either[ErrInvalidCursor, CommentCursor] = {
    String(bytes, UTF_8).decode[CommentCursor] match {
      case Right(payload) => Right(payload)
      case Left(error)    => Left(ErrInvalidCursor())
    }
  }

  extension (c: CommentCursor) {
    def encodeBase64: String = Base64.getUrlEncoder.encodeToString(c.json.getBytes(UTF_8))
  }
}
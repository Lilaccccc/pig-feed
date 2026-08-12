package internal.relation.entity

import internal.infra.errors.*
import io.circe.Codec
import utils.base.{decode, json}

import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.util.Base64

// 保存关系列表分页需要的排序字段。
final case class ListCursor(
  followedAt: LocalDateTime,
  userId: Long
) derives Codec

object ListCursor {
  def decodeBytes(bytes: Array[Byte]): Either[ErrInvalidCursor, ListCursor] = {
    String(bytes, UTF_8).decode[ListCursor] match {
      case Right(payload) => Right(payload)
      case Left(error)    => Left(ErrInvalidCursor())
    }
  }

  extension (m: ListCursor) {
    def encodeBase64: String = Base64.getUrlEncoder.encodeToString(m.json.getBytes(UTF_8))
  }
}

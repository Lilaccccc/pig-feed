package internal.feed.entity

import internal.infra.errors.*
import io.circe.Codec
import utils.base.{decode, json}

import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.util.Base64

// 保存时间线分页所需的排序字段。
final case class TimelineCursor(
  publishedAt: LocalDateTime,
  videoId: Long
) derives Codec

object TimelineCursor {
  def decodeBytes(bytes: Array[Byte]): Either[ErrInvalidCursor, TimelineCursor] = {
    String(bytes, UTF_8).decode[TimelineCursor] match {
      case Right(payload) => Right(payload)
      case Left(error)    => Left(ErrInvalidCursor())
    }
  }

  extension (t: TimelineCursor) {
    def encodeBase64: String = Base64.getUrlEncoder.encodeToString(t.json.getBytes(UTF_8))
  }
}

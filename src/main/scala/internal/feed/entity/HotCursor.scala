package internal.feed.entity

import internal.infra.errors.*
import io.circe.Codec
import utils.base.{decode, json}

import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.util.Base64

// 保存热榜分页所需的排序字段。
final case class HotCursor(
  hotScore: Option[Int] = None,
  publishedAt: Option[LocalDateTime] = None,
  videoId: Option[Long] = None,
  windowEnd: Option[LocalDateTime] = None,
  offset: Option[Int] = None
) derives Codec

object HotCursor {
  def decodeBytes(bytes: Array[Byte]): Either[ErrInvalidCursor, HotCursor] = {
    String(bytes, UTF_8).decode[HotCursor] match {
      case Right(payload) => Right(payload)
      case Left(error)    => Left(ErrInvalidCursor())
    }
  }
  
  extension (h: HotCursor) {
    def encodeBase64: String = Base64.getUrlEncoder.encodeToString(h.json.getBytes(UTF_8))
  }
}

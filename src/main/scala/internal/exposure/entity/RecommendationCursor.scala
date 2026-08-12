package internal.exposure.entity

import internal.infra.errors.*
import io.circe.Codec
import utils.base.{decode, json}
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.util.Base64

final case class RecommendationCursor(
  rankScore: Double,
  publishedAt: LocalDateTime,
  videoId: Long
) derives Codec

object RecommendationCursor {
  def decodeBytes(bytes: Array[Byte]): Either[ErrInvalidCursor, RecommendationCursor] = {
    String(bytes, UTF_8).decode[RecommendationCursor] match {
      case Right(payload) => Right(payload)
      case Left(error)    => Left(ErrInvalidCursor())
    }
  }

  extension (t: RecommendationCursor) {
    def encodeBase64: String = Base64.getUrlEncoder.encodeToString(t.json.getBytes(UTF_8))
  }
}
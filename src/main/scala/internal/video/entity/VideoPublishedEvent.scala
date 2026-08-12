package internal.video.entity

import internal.infra.rabbitmq.entity.{Event, EventId, EventTrait}
import io.circe.Codec
import utils.base.config.enums.MqConfig
import utils.base.config.enums.MqConfig.Exchange

import java.time.LocalDateTime

final case class VideoPublishedEvent(
  eventId: EventId = EventId.apply(),
  videoId: Long,
  authorId: Long,
  title: String,
  description: String,
  mediaUrl: String,
  coverUrl: String,
  publishedAt: LocalDateTime,
  occurredAt: LocalDateTime = LocalDateTime.now
) extends EventTrait derives Codec

object VideoPublishedEvent {
  extension (v: Video) {
    def toEvent: Option[Event[VideoPublishedEvent]] = {
      if v.id.isEmpty || v.id.get <= 0 then return None
      val event = Event(
        MqConfig.videoExchange,
        MqConfig.videoPublishedQueue,
        MqConfig.videoPublishedRouting,
        VideoPublishedEvent(
          videoId = v.id.get,
          authorId = v.authorId,
          title = v.title,
          description = v.description,
          mediaUrl = v.mediaUrl,
          coverUrl = v.coverUrl,
          publishedAt = v.publishedAt
        )
      )
      Some(event)
    }
  }
}

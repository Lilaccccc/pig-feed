package internal.infra.rabbitmq.entity

import io.circe.*
import utils.base.config.enums.MqConfig.*
import utils.base.idgenerator.IdGenerator
import utils.base.json

opaque type EventId = String

object EventId {
  def apply(): EventId = IdGenerator.hex16Id(12)
  given Encoder[EventId] = Encoder[String].contramap(EventId.encodeDef)
  given Decoder[EventId] = Decoder[String].map(EventId.decodeDef)
  private def encodeDef(id: String): EventId = id
  private def decodeDef(id: String): EventId = id
  private[entity] def value(id: EventId): String = id
}

trait EventTrait {
  val eventId: EventId
}

final case class Event[T <: EventTrait](
  exchange: Exchange,
  queue: Queue,
  routingKey: Routing,
  body: T,
  timeStamp: Long = System.currentTimeMillis()
) {
  def messageBody(using Encoder[T]) = body.json.getBytes

  def messageId = EventId.value(body.eventId)
}

package internal.exposure.service.mq

import com.rabbitmq.client.{AMQP, Channel, DefaultConsumer, Envelope}
import internal.exposure.service.EmbeddingService
import internal.video.entity.VideoPublishedEvent
import utils.base.{ColoredLogger, decode}

object EmbeddingConsumer extends ColoredLogger {
  def handle(c: Channel): DefaultConsumer = new DefaultConsumer(c) {
    override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, bytes: Array[Byte]): Unit = new String(bytes)
      .decode[VideoPublishedEvent]
      .fold(err => error(s"处理 MQ 消息出错：${err.getMessage}"), EmbeddingService.generateForPublishedVideo)
  }
}

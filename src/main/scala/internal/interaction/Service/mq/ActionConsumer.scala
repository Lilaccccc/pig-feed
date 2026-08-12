package internal.interaction.Service.mq

import com.rabbitmq.client.{AMQP, Channel, DefaultConsumer, Envelope}
import internal.interaction.entity.ActionChangedEvent
import internal.interaction.enums.ActionType
import internal.interaction.repository.ActionRepository
import utils.base.{ColoredLogger, decode}

object ActionConsumer extends ColoredLogger {
  def handle(c: Channel): DefaultConsumer = new DefaultConsumer(c) {
    override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, bytes: Array[Byte]): Unit = new String(bytes)
      .decode[ActionChangedEvent]
      .flatMap(e => ActionRepository.setAction(e.userId, e.videoId, ActionType.value(e.actionType), e.active, e.idempotencyKey))
      .fold(err => error(s"处理 MQ 消息出错：${err.getMessage}"), _ => ())
  }
}

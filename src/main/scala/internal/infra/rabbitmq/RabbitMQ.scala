package internal.infra.rabbitmq

import com.rabbitmq.client.*
import com.rabbitmq.client.AMQP.BasicProperties
import internal.exposure.service.mq.EmbeddingConsumer
import internal.infra.rabbitmq.entity.{Event, EventTrait}
import internal.interaction.Service.mq.ActionConsumer
import io.circe.Encoder
import utils.base.ColoredLogger
import utils.base.config.enums.MqConfig
import utils.base.config.enums.MqConfig.*

import java.util.Date
import scala.util.{Failure, Success, Try}

object RabbitMQ extends ColoredLogger {
  lazy val consumerChannel = Try(conn.get.createChannel).toOption

  private lazy val conn = {
    val f = ConnectionFactory()
    f.setHost(MqConfig.host)         // 设置Rabbit服务的地址
    f.setPort(MqConfig.port)         // 设置Rabbit服务的端口号
    f.setUsername(MqConfig.username) // 设置Rabbit服务的账号
    f.setPassword(MqConfig.password) // 设置Rabbit服务的密码
    f.setVirtualHost("/")            // 设置Rabbit服务的虚拟主机
    f.setAutomaticRecoveryEnabled(true)
    Try(f.newConnection()) match {
      case Success(value) => Some(value)
      case Failure(err)   =>
        error(err.getMessage)
        None
    }
  }

  private lazy val publishChannel = Try(conn.get.createChannel).map(ensureTopology).toOption
  private val ensureTopology      = (channel: Channel) => {
    // 声明交换机
    Exchange.list.foreach(exchange => channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true))
    // 声明队列
    Queue.list.foreach(queue => channel.queueDeclare(queue, true, false, false, null))
    // 绑定队列到交换机
    channel.queueBind(Queue.value(actionChangedQueue), Exchange.value(interactionExchange), Routing.value(actionChangedRouting))
    channel.queueBind(Queue.value(videoPublishedQueue), Exchange.value(videoExchange), Routing.value(videoPublishedRouting))
    channel.queueBind(Queue.value(videoEmbeddingQueue), Exchange.value(videoExchange), Routing.value(videoPublishedRouting))
    channel.queueBind(Queue.value(viewEventRecordedQueue), Exchange.value(exposureExchange), Routing.value(viewEventRecordedRouting))
    channel
  }

  def close = {
    publishChannel.filter(_.isOpen).foreach(_.close())
    consumerChannel.filter(_.isOpen).foreach(_.close())
    conn.filter(_.isOpen).foreach(_.close())
  }

  def publish[T <: EventTrait](event: Event[T])(using Encoder[T]): Boolean = {
    val messageId  = event.messageId
    val timestamp  = Date(event.timeStamp)
    val props      = BasicProperties.Builder().deliveryMode(2).messageId(messageId).timestamp(timestamp).build()
    val exchange   = Exchange.value(event.exchange)
    val routingKey = Routing.value(event.routingKey)
    val publish    = (c: Channel) =>
      Try(c.basicPublish(exchange, routingKey, false, false, props, event.messageBody)) match {
        case Success(_)         => true
        case Failure(exception) =>
          error(exception.getMessage)
          false
      }
    publishChannel.filter(_.isOpen).exists(publish)
  }

  def startConsume(): Unit = consumerChannel.foreach(c => {
    val callback: Consumer = new DefaultConsumer(c) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, bytes: Array[Byte]): Unit = {
        debug(s"ConsumerTag::$consumerTag")
        debug(s"Properties::$properties")
        debug(s"Body::${new String(bytes)}")
      }
    }

    c.basicConsume(MqConfig.Queue.value(MqConfig.actionChangedQueue), true, ActionConsumer.handle(c))
    c.basicConsume(MqConfig.Queue.value(MqConfig.videoPublishedQueue), true, EmbeddingConsumer.handle(c))
    c.basicConsume(MqConfig.Queue.value(MqConfig.videoEmbeddingQueue), true, callback)
    c.basicConsume(MqConfig.Queue.value(MqConfig.viewEventRecordedQueue), true, callback)
  })
}

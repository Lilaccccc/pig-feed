package utils.base.config.enums

import utils.base.config
import utils.base.config.Config

enum MqConfig(val field: String) extends Config("mq") {
  private case Host                     extends MqConfig("config.host")
  private case Port                     extends MqConfig("config.port")
  private case Username                 extends MqConfig("config.username")
  private case Password                 extends MqConfig("config.password")
  private case InteractionExchange      extends MqConfig("default.interactionExchange")
  private case ActionChangedQueue       extends MqConfig("default.actionChangedQueue")
  private case ActionChangedRouting     extends MqConfig("default.actionChangedRouting")
  private case VideoExchange            extends MqConfig("default.videoExchange")
  private case VideoPublishedQueue      extends MqConfig("default.videoPublishedQueue")
  private case VideoEmbeddingQueue      extends MqConfig("default.videoEmbeddingQueue")
  private case VideoPublishedRouting    extends MqConfig("default.videoPublishedRouting")
  private case ExposureExchange         extends MqConfig("default.exposureExchange")
  private case ViewEventRecordedQueue   extends MqConfig("default.viewEventRecordedQueue")
  private case ViewEventRecordedRouting extends MqConfig("default.viewEventRecordedRouting")
}

object MqConfig {
  opaque type Exchange = String
  opaque type Queue = String
  opaque type Routing = String

  object Exchange {
    def apply(value: String): Exchange = value
    def value(exchange: Exchange): String = exchange
    
    def list = List(value(interactionExchange), value(videoExchange), value(exposureExchange))
  }

  object Queue {
    def apply(value: String): Queue = value
    def value(queue: Queue): String = queue

    def list = List(value(actionChangedQueue), value(videoPublishedQueue), value(videoEmbeddingQueue), value(viewEventRecordedQueue))
  }

  object Routing {
    def apply(value: String): Routing = value
    def value(routing: Routing): String = routing
  }
  
  def host                              = config.get[String](Host)
  def port                              = config.get[Int](Port)
  def username                          = config.get[String](Username)
  def password                          = config.get[String](Password)
  
  def interactionExchange: Exchange     = Exchange(config.get[String](InteractionExchange))
  def actionChangedQueue: Queue         = Queue(config.get[String](ActionChangedQueue))
  def actionChangedRouting: Routing     = Routing(config.get[String](ActionChangedRouting))
  def videoExchange: Exchange           = Exchange(config.get[String](VideoExchange))
  def videoPublishedQueue: Queue        = Queue(config.get[String](VideoPublishedQueue))
  def videoEmbeddingQueue: Queue        = Queue(config.get[String](VideoEmbeddingQueue))
  def videoPublishedRouting: Routing    = Routing(config.get[String](VideoPublishedRouting))
  def exposureExchange: Exchange        = Exchange(config.get[String](ExposureExchange))
  def viewEventRecordedQueue: Queue     = Queue(config.get[String](ViewEventRecordedQueue))
  def viewEventRecordedRouting: Routing = Routing(config.get[String](ViewEventRecordedRouting))
}

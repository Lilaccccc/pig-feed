package internal

import internal.infra.rabbitmq.RabbitMQ
import internal.upload.utils.prepareUploadDirectory
import org.apache.pekko.http.cors.scaladsl.CorsDirectives.cors
import org.apache.pekko.http.scaladsl.server.Directives.*
import utils.base.ColoredLogger
import utils.base.config.enums.UploadConfig
import utils.db.migrate
import utils.redis.{RedisFactory, RedisOps}
import utils.route.{Controller, HttpService, Routes, initControllers}

import java.nio.file.Paths

object InitService extends ColoredLogger {
  def apply: Unit = {
    initFile
    initDb
    startMqConsume
    RedisOps.init
    initHttp
  }

  private def initFile: Unit = prepareUploadDirectory(using this)
  
  private def initDb: Unit = migrate()

  private def initHttp: Unit = {
    val controllers = initControllers[Controller]
//    debug(s"初始化路由：$controllers")
    val endpointList = controllers.flatMap(_.serverEndpointList)
//    debug(s"初始化端点：${endpointList.map(ep => s"${ep.endpoint.showShort}").mkString("[", ", ", "]")}")
    // 上传文件以 /uploads/{kind}/{filename} 形式访问，由静态文件路由直接从本地目录读取。
    // 转为绝对路径，避免服务器工作目录不同时找不到文件。
    val uploadRoot = Paths.get(UploadConfig.root).toAbsolutePath.toString
    val staticRoute = pathPrefix("uploads") {
      getFromDirectory(uploadRoot)
    }
    HttpService.init(cors()(staticRoute ~ Routes.toRoute(endpointList)), closeTask)
  }

  private def closeTask = () => {
    RedisFactory.shutdown
    RabbitMQ.close
  }
  
  private def startMqConsume = Thread.ofVirtual.start(() => RabbitMQ.startConsume())
}

package utils.route

import sttp.tapir.Tapir
import sttp.tapir.json.circe.TapirJsonCirce
import sttp.tapir.server.ServerEndpoint
import utils.base.ColoredLogger

import scala.concurrent.Future

// 控制层特质
trait Controller extends Tapir, TapirJsonCirce, ColoredLogger {
  // 暴露服务端点
  val serverEndpointList: List[ServerEndpoint[Any, Future]]
}
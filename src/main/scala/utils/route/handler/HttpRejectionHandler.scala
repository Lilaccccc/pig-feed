package utils.route.handler

import io.circe.syntax.*
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives.{complete, extractRequest}
import org.apache.pekko.http.scaladsl.server.RejectionHandler
import utils.base.ColoredLogger
import utils.result.{R, StatusCode}

object HttpRejectionHandler extends ColoredLogger {
  // 提供隐式的请求拒绝处理器
  given RejectionHandler = RejectionHandler
    .newBuilder()
    .handleNotFound {
      extractRequest { request =>
        val path = request.uri.path.toString
        error(s"404 Not Found: $path")
        complete(
          StatusCodes.OK,
          HttpEntity(
            ContentTypes.`application/json`,
            R.status(StatusCode.NotFound).asJson.toString
          )
        )
      }
    }
    .result()
}

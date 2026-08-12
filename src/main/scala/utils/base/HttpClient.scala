package utils.base

import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.model.Uri.Query
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.util.ByteString
import utils.route.HttpService.{ec, system}

import scala.concurrent.Future

object HttpClient {
  // 只检查服务是否可达（不解析响应体）
  def head(
    url: String,
    headers: Map[String, String] = Map.empty
  ): Future[Boolean] = {
    val request = HttpRequest(
      method = HttpMethods.HEAD, // 使用 HEAD 方法，不返回响应体
      uri = url,
      headers = headers.map { case (name, value) => RawHeader(name, value) }.toList
    )

    Http()
      .singleRequest(request)
      .map(response => response.status.isSuccess)
      .recover(_ => false)
  }

  // GET 请求
  def sendGetRequest[OUT: Decoder](
    url: String,
    queryParams: Map[String, String] = Map.empty,
    headers: Map[String, String] = Map.empty
  ): Future[Option[OUT]] = {
    // 构建 HttpRequest 对象，并且正确编码查询参数
    val uri = Uri(url).withQuery(Query(queryParams))
    // 构建带请求头的 GET 请求体
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = uri,
      headers = headers.map { case (name, value) =>
        RawHeader(name, value)
      }.toList
    )

    handle(request)
  }

  private def handle[OUT: Decoder](
    request: HttpRequest
  ): Future[Option[OUT]] = Http().singleRequest(request).flatMap { response =>
    if response.status.isSuccess() then {
      response.entity.contentLengthOption match {
        case Some(length) if length > 0 =>
          response.entity.dataBytes
            .runFold(ByteString(""))(_ ++ _)
            .map(_.utf8String)
            .map(jsonString => decode[OUT](jsonString).toOption)
        // 没有响应体，返回None
        case _ => Future.successful(None)
      }
    } else Future.failed(RuntimeException(s"Request failed with status: ${response.status}"))
  }

  // POST 请求
  def sendPostRequest[IN: Encoder, OUT: Decoder](
    url: String,
    body: Option[IN] = None,
    headers: Map[String, String] = Map.empty
  ): Future[Option[OUT]] = {
    // 构建请求体，根据 body 是否存在决定请求实体
    val entity = body match {
      case Some(b) =>
        // 将 IN 类型转换为 JSON 字符串
        val jsonBody = b.asJson.noSpaces
        HttpEntity(ContentTypes.`application/json`, jsonBody)
      case None =>
        HttpEntity.Empty // 无 body 的请求
    }
    // 构建 HttpRequest 对象
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = url,
      headers = headers.map { case (name, value) => RawHeader(name, value) }.toList,
      entity = entity
    )
    handle(request)
  }
}

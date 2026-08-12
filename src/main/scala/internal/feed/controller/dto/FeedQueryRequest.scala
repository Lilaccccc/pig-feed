package internal.feed.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 复杂 Feed 查询入口的请求体。
final case class FeedQueryRequest(
  scene: String,
  cursor: String,
  limit: Int,
  clientContext: Map[String, String] = Map.empty
) derives Codec, Schema

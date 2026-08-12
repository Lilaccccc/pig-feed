package internal.video.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 发布视频的 JSON 请求体。
final case class CreateVideoRequest(
  title: String,
  description: Option[String] = None,
  mediaUrl: String,
  coverUrl: String
) derives Codec, Schema
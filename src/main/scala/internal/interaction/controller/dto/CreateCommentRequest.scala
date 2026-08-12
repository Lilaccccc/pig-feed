package internal.interaction.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 创建评论的 JSON 请求体。
final case class CreateCommentRequest(content: String) derives Codec, Schema

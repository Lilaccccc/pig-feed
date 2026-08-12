package internal.interaction.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 删除评论后的状态响应
final case class DeleteCommentResponse(
  commentId: Long,
  status: Int,
  commentCount: Long
) derives Codec, Schema

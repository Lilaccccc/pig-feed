package internal.interaction.controller.dto

import internal.interaction.controller.dto.CommentResponse.commentResponseFromDomain
import internal.interaction.entity.CommentListResult
import io.circe.Codec
import sttp.tapir.Schema

// 评论游标分页响应
final case class CommentListResponse(
  items: List[CommentResponse],
  nextCursor: String,
  hasMore: Boolean
) derives Codec, Schema

object CommentListResponse {
  extension (result: CommentListResult) {
    def commentListResponseFromResult = CommentListResponse(
      result.items.map(_.commentResponseFromDomain(None)),
      result.nextCursor,
      result.hasMore
    )
  }
}

package internal.relation.controller.dto

import internal.relation.controller.dto.RelationUserResponse.relationUserResponseFromResult
import internal.relation.entity.ListResult
import io.circe.Codec
import sttp.tapir.Schema

// 关系列表游标分页响应
final case class RelationListResponse(
  items: List[RelationUserResponse],
  nextCursor: String,
  hasMore: Boolean
) derives Codec, Schema

object RelationListResponse {
  extension (list: ListResult) {
    def relationListResponseFromResult: RelationListResponse = RelationListResponse(
      list.items.map(relationUserResponseFromResult),
      list.nextCursor,
      list.hasMore
    )
  }
}

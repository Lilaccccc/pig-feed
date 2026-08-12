package internal.message.controller.dto

import internal.message.entity.ListResult
import io.circe.Codec
import sttp.tapir.Schema

final case class MessageListResponse(
  items: List[MessageResponse],
  nextCursor: String,
  hasMore: Boolean
) derives Codec, Schema

object MessageListResponse {
  extension (list: ListResult) {
    def responseFromDomain = {
      val items = list.items.map(MessageResponse.responseFromDomain)
      MessageListResponse(items, list.nextCursor, list.hasMore)
    }
  }
}

package internal.feed.controller.dto

import internal.feed.entity.*
import internal.feed.enums.Scene
import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

// Feed 游标分页响应。
final case class FeedItemsResponse(
  scene: String,
  items: List[FeedItemResponse],
  nextCursor: String,
  hasMore: Boolean
) derives Codec, Schema

object FeedItemsResponse {
  extension (result: FeedResult) {
    def feedItemsResponseFromResult: FeedItemsResponse = {
      val itemResp = (it: FeedItem) =>
        FeedItemResponse(
          it.videoId,
          it.authorId,
          it.authorNickname.trim,
          it.authorAvatarUrl.trim,
          it.title.trim,
          it.description,
          it.mediaUrl.trim,
          it.coverUrl.trim,
          it.likeCount,
          it.commentCount,
          it.favoriteCount,
          it.liked.getOrElse(false),
          it.favorited.getOrElse(false),
          it.following.getOrElse(false),
          it.publishedAt
        )
      FeedItemsResponse(
        result.scene.value,
        result.items.map(itemResp),
        result.nextCursor,
        result.hasMore
      )
    }
  }
}

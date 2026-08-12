package internal.relation.controller.dto

import internal.relation.entity.UserItem
import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

// 关注列表和粉丝列表中的用户项
final case class RelationUserResponse(
  userId: Long,
  nickname: String,
  avatarUrl: String,
  bio: String,
  followedAt: LocalDateTime
) derives Codec, Schema

object RelationUserResponse {
  extension (item: UserItem) {
    def relationUserResponseFromResult: RelationUserResponse = RelationUserResponse(
      item.userId,
      item.nickname,
      item.avatarUrl,
      item.bio,
      item.followedAt
    )
  }
}

package internal.interaction.controller.dto

import internal.interaction.entity.ActionResult
import io.circe.Codec
import sttp.tapir.Schema

// 点赞/收藏状态变更后的响应。
final case class ActionResponse(
  videoId: Long,
  actionType: String,
  active: Boolean,
  likeCount: Long,
  favoriteCount: Long
) derives Codec, Schema

object ActionResponse {
  extension (result: ActionResult) {
    // 把应用层点赞/收藏结果转换为 HTTP 响应。
    def actionResponseFromResult = ActionResponse(
      result.videoId,
      result.actionType,
      result.active,
      result.likeCount,
      result.favoriteCount
    )
  }
}

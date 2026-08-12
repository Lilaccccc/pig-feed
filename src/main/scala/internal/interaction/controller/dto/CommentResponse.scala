package internal.interaction.controller.dto

import internal.interaction.entity.{Comment, CreateCommentResult}
import io.circe.Codec
import sttp.tapir.Schema

import java.time.LocalDateTime

// 评论详情响应，创建评论时会额外返回 CommentCount。
final case class CommentResponse(
  id: Long,
  videoId: Long,
  userId: Long,
  userNickname: String,
  userAvatarUrl: String,
  content: String,
  createdAt: LocalDateTime,
  commentCount: Option[Long] = None
) derives Codec, Schema

object CommentResponse {
  extension (comment: Comment) {
    def commentResponseFromDomain(commentCount: Option[Long]) = CommentResponse(
      comment.id,
      comment.videoId,
      comment.userId,
      comment.userNickname,
      comment.userAvatarUrl,
      comment.content,
      comment.createdAt,
      commentCount
    )
  }
}

package internal.interaction.controller

import internal.infra.route.RouteOps.*
import internal.interaction.controller.dto.*
import internal.interaction.controller.handler.InteractionHandler
import internal.interaction.enums.ActionType
import utils.result.R
import utils.route.Controller

class InteractionController extends Controller {

  override val serverEndpointList = List(like, unLike, favorite, unFavorite, createComment, listComments, deleteComment)

  // /videos/like/{videoId}
  def like = this.endpoint.put
    .in("videos" / "like" / query[Long]("videoId") and header[String]("Idempotency-Key"))
    .out(jsonBody[R[ActionResponse]])
    .security((c: Context, body: (videoId: Long, idempotencyKey: String)) =>
      InteractionHandler
        .likeOrFavorite(c.userId, body.videoId, body.idempotencyKey, true, ActionType.Like)
    )

  // /videos/unlike/{videoId}
  def unLike = this.endpoint.delete
    .in("videos" / "unlike" / query[Long]("videoId") and header[String]("Idempotency-Key"))
    .out(jsonBody[R[ActionResponse]])
    .security((c: Context, body: (videoId: Long, idempotencyKey: String)) =>
      InteractionHandler
        .likeOrFavorite(c.userId, body.videoId, body.idempotencyKey, false, ActionType.Like)
    )

  // /videos/favorite/{videoId}
  def favorite = this.endpoint.put
    .in("videos" / "favorite" / query[Long]("videoId") and header[String]("Idempotency-Key"))
    .out(jsonBody[R[ActionResponse]])
    .security((c: Context, body: (videoId: Long, idempotencyKey: String)) =>
      InteractionHandler
        .likeOrFavorite(c.userId, body.videoId, body.idempotencyKey, true, ActionType.Favorite)
    )

  // /videos/unfavorite/{videoId}
  def unFavorite = this.endpoint.delete
    .in("videos" / "unfavorite" / query[Long]("videoId") and header[String]("Idempotency-Key"))
    .out(jsonBody[R[ActionResponse]])
    .security((c: Context, body: (videoId: Long, idempotencyKey: String)) =>
      InteractionHandler
        .likeOrFavorite(c.userId, body.videoId, body.idempotencyKey, false, ActionType.Favorite)
    )

  // /videos/createcomments/{videoId}
  def createComment = this.endpoint.post
    .in("videos" / "createcomments" / query[Long]("videoId") and header[String]("Idempotency-Key") and jsonBody[CreateCommentRequest])
    .out(jsonBody[R[CommentResponse]])
    .security((c: Context, body: (videoId: Long, idempotencyKey: String, request: CreateCommentRequest)) =>
      InteractionHandler
        .createComment(c.userId, body.videoId, body.idempotencyKey, body.request)
    )

  // /videos/listcomments/{videoId}
  def listComments = this.endpoint.get
    .in("videos" / "listcomments" / query[Long]("videoId") and query[Int]("limit") and query[String]("cursor").default(""))
    .out(jsonBody[R[CommentListResponse]])
    .logic((videoId: Long, limit: Int, cursor: String) => InteractionHandler.listComments(videoId, limit, cursor))

  // /comments/delete/{commentId}
  // 删除评论只需要评论自身 ID，所以放在顶层 comments 资源下。
  def deleteComment = this.endpoint.delete
    .in("comments" / "delete" / query[Long]("commentId"))
    .out(jsonBody[R[DeleteCommentResponse]])
    .security((c: Context, commentId: Long) => InteractionHandler.deleteComment(c.userId, c.role, commentId))
}

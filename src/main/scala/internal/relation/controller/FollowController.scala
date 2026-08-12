package internal.relation.controller

import internal.infra.route.RouteOps.*
import internal.relation.controller.dto.*
import internal.relation.controller.handler.FollowHandler
import utils.result.R
import utils.route.Controller

class FollowController extends Controller {
  override val serverEndpointList = List(follow, unFollow, following, followers)

  // /users/me/follow
  private def follow = this.endpoint.put
    .in("users" / "me" / "follow")
    .in(path[Long]("targetUserId") and header[String]("Idempotency-Key"))
    .out(jsonBody[R[FollowResponse]])
    .security((c: Context, body: (targetUserId: Long, idempotencyKey: String)) => FollowHandler.follow(c.userId, body.targetUserId, true, body.idempotencyKey))

  // /users/me/unfollow
  private def unFollow = this.endpoint.delete
    .in("users" / "me" / "unfollow")
    .in(path[Long]("targetUserId") and header[String]("Idempotency-Key"))
    .out(jsonBody[R[FollowResponse]])
    .security((c: Context, body: (targetUserId: Long, idempotencyKey: String)) => FollowHandler.follow(c.userId, body.targetUserId, false, body.idempotencyKey))

  // /users/me/following/list
  private def following = this.endpoint.get
    .in("users" / "me" / "following" / "list")
    .in(query[String]("cursor") and query[Int]("limit"))
    .out(jsonBody[R[RelationListResponse]])
    .security((c: Context, body: (cursor: String, limit: Int)) => FollowHandler.listFollowing(c.userId, body.cursor, body.limit))

  // /users/me/followers/list
  private def followers = this.endpoint.get
    .in("users" / "me" / "followers" / "list")
    .in(query[String]("cursor") and query[Int]("limit"))
    .out(jsonBody[R[RelationListResponse]])
    .security((c: Context, body: (cursor: String, limit: Int)) => FollowHandler.listFollowers(c.userId, body.cursor, body.limit))
}

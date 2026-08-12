package internal.video.controller

import internal.infra.route.RouteOps.*
import internal.video.controller.dto.*
import internal.video.controller.handler.UsersVideoHandler as VideoHandler
import utils.result.R
import utils.route.Controller

class UsersVideoController extends Controller {
  override val serverEndpointList = List(create, get, delete, listByAuthor, listByMine)

  // /users/create/videos
  private def create = this.endpoint.post
    .in("users" / "create" / "videos" and header[String]("Idempotency-Key") and jsonBody[CreateVideoRequest])
    .out(jsonBody[R[VideoResponse]])
    .security { (c: Context, body: (idempotencyKey: String, request: CreateVideoRequest)) =>
      VideoHandler.create(body.idempotencyKey, c.userId, body.request)
    }

  // /users/videos/get/{videoId}
  private def get = this.endpoint.get
    .in("users" / "videos" / "get" and path[Long]("videoId"))
    .out(jsonBody[R[VideoResponse]])
    .security((_, videoId: Long) => VideoHandler.get(videoId))

  // /users/videos/delete/{videoId}
  private def delete = this.endpoint.delete
    .in("users" / "videos" / "delete" and path[Long]("videoId"))
    .out(jsonBody[R[Long]])
    .security((c: Context, videoId: Long) => VideoHandler.delete(c.userId, videoId))

  // /users/{userId}/videos
  private def listByAuthor = this.endpoint.get
    .in("users" / path[Long]("userId") / "videos")
    .in(query[Int]("limit").default(20) and query[Int]("offset").default(0))
    .out(jsonBody[R[VideoListResponse]])
    .logic((userId: Long, limit: Int, offset: Int) => VideoHandler.listByAuthor(userId, limit, offset))

  // /users/me/videos/page
  private def listByMine = this.endpoint.get
    .in("users" / "me" / "videos" / "page")
    .in(query[Int]("limit").default(20) and query[Int]("offset").default(0))
    .out(jsonBody[R[VideoListResponse]])
    .security((c: Context, body: (limit: Int, offset: Int)) => VideoHandler.listByAuthor(c.userId, body.limit, body.offset))
}

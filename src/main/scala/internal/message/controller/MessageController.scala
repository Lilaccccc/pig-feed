package internal.message.controller

import internal.infra.route.RouteOps.*
import internal.message.controller.dto.*
import internal.message.controller.handler.MessageHandler
import utils.result.R
import utils.route.Controller

class MessageController extends Controller {

  override val serverEndpointList = List(list, markRead, countUnread)

  // /messages/list
  private def list = this.endpoint.get
    .in("messages" / "list" and query[Int]("limit") and query[String]("cursor"))
    .out(jsonBody[R[MessageListResponse]])
    .security((c: Context, body: (limit: Int, cursor: String)) => MessageHandler.list(c.userId, body.cursor, body.limit))

  // /messages/markread
  private def markRead = this.endpoint.patch
    .in("messages" / "markread" and jsonBody[MarkReadRequest])
    .out(jsonBody[R[MarkReadResponse]])
    .security((c: Context, request: MarkReadRequest) => MessageHandler.markRead(c.userId, request))

  // /messages/countunread
  private def countUnread = this.endpoint.get
    .in("messages-stats" / "countunread")
    .out(jsonBody[R[UnreadStatResponse]])
    .security((c: Context, _) => MessageHandler.countUnread(c.userId))
}

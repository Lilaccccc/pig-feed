package internal.feed.controller

import internal.feed.controller.dto.*
import internal.feed.controller.handler.FeedHandler
import internal.infra.route.RouteOps.*
import utils.result.R
import utils.route.Controller

class FeedController extends Controller {
  private type FeedsItemsRequest = (limit: Int, scene: String, cursor: String)

  override val serverEndpointList = List(feedsItems, feedsQueries, refresh)

  // /feeds/items
  def feedsItems = this.endpoint.get
    .in("feeds" / "items" and query[Int]("limit") and query[String]("scene") and query[String]("cursor"))
    .out(jsonBody[R[FeedItemsResponse]])
    .security((c: Context, body: FeedsItemsRequest) => FeedHandler.listFeedItems(c.userId, body.limit, body.scene, body.cursor))

  // /feeds/queries
  def feedsQueries = this.endpoint.post
    .in("feeds" / "queries" and jsonBody[FeedQueryRequest])
    .out(jsonBody[R[FeedItemsResponse]])
    .security((c: Context, body: FeedQueryRequest) => FeedHandler.query(c.userId, body))

  // /feeds/refresh
  def refresh = this.endpoint.get
    .in("feeds" / "refresh" and query[Int]("limit"))
    .out(jsonBody[R[FeedItemsResponse]])
    .logic(limit => FeedHandler.refresh(limit))
}

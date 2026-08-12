package internal.feed.controller.handler

import internal.feed.controller.dto.*
import internal.feed.controller.dto.FeedItemsResponse.*
import internal.feed.entity.*
import internal.feed.service.FeedService
import utils.base.ColoredLogger
import utils.result.throws

object FeedHandler extends ColoredLogger {
  // 读取指定 scene 的 Feed，cursor 和 limit 来自 query 参数。
  def listFeedItems(viewerId: Long, limit: Int, scene: String, cursor: String) = {
    val req = FeedRequest(scene, cursor, limit, Some(viewerId))
    FeedService.getFeed(req).fold(err => err.throws(using this), feedItemsResponseFromResult)
  }

  // 通过请求体接收复杂 Feed 查询参数，适合推荐上下文逐步扩展。
  def query(viewerId: Long, request: FeedQueryRequest) = {
    val req = FeedRequest(request.scene, request.cursor, request.limit, Some(viewerId), request.clientContext)
    FeedService.getFeed(req).fold(err => err.throws(using this), feedItemsResponseFromResult)
  }

  // 从第一页重新读取 Feed，适合下拉刷新语义。
  def refresh(limit: Int) = FeedService.refreshFeed(limit).fold(err => err.throws(using this), feedItemsResponseFromResult)
}

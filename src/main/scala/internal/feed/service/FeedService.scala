package internal.feed.service

import internal.feed.entity.*
import internal.feed.enums.Scene.*
import utils.base.ColoredLogger

// 通过 scene 策略注册表分发不同 Feed 场景。
object FeedService extends ColoredLogger {
  // 根据 scene 分发到对应策略。
  def getFeed(request: FeedRequest): Either[Exception, FeedResult] = request.strategy.list(request)

  // 使用 cursor + limit 快捷读取时间线 Feed。
  def getTimelineFeed(cursor: String, limit: Int): Either[Exception, FeedResult] = this.getFeed(FeedRequest(Timeline.value, cursor, limit))

  // 从第一页重新加载默认 Feed，适合下拉刷新场景。
  def refreshFeed(limit: Int): Either[Exception, FeedResult] = this.getTimelineFeed("", limit)
}

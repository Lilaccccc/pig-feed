package internal.feed.service.strategy

import internal.feed.entity.*
import internal.feed.entity.HotCursor.*
import internal.feed.enums.Scene
import internal.feed.enums.Scene.Hot
import internal.feed.repository.InboxRepository
import internal.feed.service.cache.FeedCacheService
import internal.feed.utils.decodeCursor
import utils.base.ColoredLogger

import java.time.temporal.ChronoUnit.MINUTES
import java.time.{LocalDateTime, ZoneOffset}

// 使用互动热度读取热榜 Feed。
object HotStrategy extends Strategy, ColoredLogger {

  override def scene: Scene = Hot

  // 读取热榜；Redis 场景使用最近一小时分钟桶，基础场景使用仓储累计热度。
  override def list(request: FeedRequest): Either[Exception, FeedResult] = {
    val cursor = request.cursor
    val limit  = request.normalizeLimit

    val feedResult = (page: FeedPage) => try {
      val feedItems = super.assembleFeedItems(request.viewerId, page.items)
      FeedResult(scene, feedItems, page.nextCursor, page.hasMore)
    } catch {
      case e: Exception => error(e.getMessage); FeedResult(scene, List.empty, "", false)
    }

    val result = (cursorOpt: Option[HotCursor]) => {
      val page = listPageFromHotWindow(cursorOpt, limit)
      feedResult(if page.items.isEmpty then listPageFromRepo(cursorOpt, limit) else page)
    }

    parseHotCursor(request.cursor).fold(err => Left(err), opt => Right(result(opt)))
  }

  // 将客户端传回的热榜游标解析成领域游标。
  private def parseHotCursor(raw: String): Either[Exception, Option[HotCursor]] = {
    if raw.isBlank then return Right(None)
    decodeCursor(raw)
      .flatMap(bytes => HotCursor.decodeBytes(bytes))
      .fold(err => Left(err), result => Right(Some(result)))
  }

  private def listPageFromHotWindow(cursor: Option[HotCursor], limit: Int): FeedPage = {
    val (windowEnd, offset) = cursor
      .filter(_.windowEnd.nonEmpty)
      .map(c => (c.windowEnd.get.truncatedTo(MINUTES), c.offset.getOrElse(0)))
      .getOrElse((LocalDateTime.now.truncatedTo(MINUTES), 0))

    val list    = FeedCacheService.listHotWindowPage(windowEnd.toInstant(ZoneOffset.UTC), offset, limit + 1)
    val hasMore = list.size > limit
    val items   = if hasMore then list.slice(0, limit) else list

    val nextCursor = if items.nonEmpty then {
      val hotCursor = HotCursor(
        windowEnd = Some(windowEnd),
        offset = Some(offset + items.length)
      )
      encodeHotWindowCursor(Some(hotCursor))
    } else ""

    FeedPage(scene, items, nextCursor, hasMore)
  }

  private def listPageFromRepo(cursor: Option[HotCursor], limit: Int): FeedPage = {
    val list    = InboxRepository.listHotPage(cursor, limit + 1)
    val hasMore = list.length > limit
    val items   = if hasMore then list.slice(0, limit) else list

    val nextCursor = if items.nonEmpty then {
      val last      = items.last
      val hotCursor = HotCursor(
        hotScore = last.hotScore,
        publishedAt = last.publishedAt,
        videoId = Some(last.videoId)
      )
      encodeHotWindowCursor(Some(hotCursor))
    } else ""

    FeedPage(scene, items, nextCursor, hasMore)
  }

  // 把排序字段编码成 URL 安全的游标字符串。
  private def encodeHotWindowCursor(cursorOpt: Option[HotCursor]): String = {
    cursorOpt.filter(_.videoId.getOrElse(0L) > 0).map(encodeBase64).getOrElse("")
  }
}

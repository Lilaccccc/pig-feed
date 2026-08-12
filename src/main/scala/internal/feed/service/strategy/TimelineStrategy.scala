package internal.feed.service.strategy

import internal.feed.entity.*
import internal.feed.entity.TimelineCursor.encodeBase64
import internal.feed.enums.FeedCache.*
import internal.feed.enums.Scene
import internal.feed.enums.Scene.Timeline
import internal.feed.repository.InboxRepository
import internal.feed.service.cache.FeedCacheService
import internal.feed.utils.{SingleFlight, decodeCursor}
import utils.base.ColoredLogger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

// 复用现有时间线查询能力。
object TimelineStrategy extends Strategy, ColoredLogger {
  private type LoadFeedPageParam = (cursor: String, limit: Int, load: () => FeedPage)
  private val group = SingleFlight[String, FeedPage]()

  // 使用 cursor+limit 读取时间线 Feed。
  override def list(request: FeedRequest): Either[Exception, FeedResult] = {
    debug(s"request::$request")
    val cursor = request.cursor
    val limit  = request.normalizeLimit

    val feedResult = (page: FeedPage) => try {
      debug(s"page.items::${page.items}")
      val feedItems = super.assembleFeedItems(request.viewerId, page.items)
      debug(s"feedItems::$feedItems")
      FeedResult(scene, feedItems, page.nextCursor, page.hasMore)
    } catch {
      case e: Exception => error(e.getMessage); FeedResult(scene, List.empty, "", false)
    }

    val result = (cursorOpt: Option[TimelineCursor]) => {
      val param  = (cursor, limit, () => listPageFromRepo(cursorOpt, limit))
      val task   = loadFeedPage(param).map(feedResult)
      val result = Await.result(task, 2.seconds)
      debug(result.toString)
      result
    } 

    parseTimelineCursor(cursor).fold(err => Left(err), opt => Right(result(opt)))
  }

  // 将客户端传回的字符串游标解析成领域游标。
  private def parseTimelineCursor(raw: String): Either[Exception, Option[TimelineCursor]] = {
    if raw.isBlank then return Right(None)
    decodeCursor(raw)
      .flatMap(bytes => TimelineCursor.decodeBytes(bytes))
      .fold(err => Left(err), result => Right(Some(result)))
  }

  private def loadFeedPage(param: LoadFeedPageParam): Future[FeedPage] = {
    val cacheKey  = feedPageCacheKey(scene, param.cursor, param.limit)
    val cachePage = Future {
      FeedCacheService.getPage(cacheKey).getOrElse {
        val page = param.load()
        debug(s"page::$page")
        val ttl = feedPageCacheTtl(param.cursor, cacheKey, TimelineFirstPageCacheTTL.value, TimelinePageCacheTTL.value)
        FeedCacheService.setPage(cacheKey, page, ttl)
        page
      }
    }
    group.doOnce(cacheKey)(cachePage)
  }

  override def scene: Scene = Timeline

  private def listPageFromRepo(cursor: Option[TimelineCursor], limit: Int): FeedPage = {
    // limit+1 是常见分页技巧：多取一条即可判断后面还有没有数据。
    val list = InboxRepository.listTimelinePage(cursor, limit + 1)
    debug(s"list::$list")
    val hasMore = list.length > limit
    val items   = if hasMore then list.slice(0, limit) else list

    val nextCursor = if items.nonEmpty then {
      val last           = items.last
      val timelineCursor = TimelineCursor(
        last.publishedAt.get,
        last.videoId
      )
      encodeTimelineCursor(Some(timelineCursor))
    } else ""

    debug(FeedPage(scene, items, nextCursor, hasMore).toString)

    FeedPage(scene, items, nextCursor, hasMore)
  }

  // 把排序字段编码成 URL 安全的游标字符串。
  private def encodeTimelineCursor(cursorOpt: Option[TimelineCursor]): String = {
    cursorOpt.filter(_.videoId > 0).map(encodeBase64).getOrElse("")
  }
}

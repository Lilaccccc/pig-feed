package internal.feed.service.strategy

import internal.feed.entity.*
import internal.feed.entity.TimelineCursor.encodeBase64
import internal.feed.enums.FeedCache.*
import internal.feed.enums.Scene
import internal.feed.enums.Scene.Following
import internal.feed.repository.InboxRepository
import internal.feed.service.cache.FeedCacheService
import internal.feed.utils.{SingleFlight, decodeCursor}
import utils.base.ColoredLogger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

// 按关注关系读取时间线 Feed：查出关注作者的已发布视频，按发布时间倒序稳定分页。
object FollowingStrategy extends Strategy, ColoredLogger {
  private type LoadFeedPageParam = (viewerId: Long, cursor: String, limit: Int, load: () => FeedPage)
  private val group = SingleFlight[String, FeedPage]()

  override def scene: Scene = Following

  override def list(request: FeedRequest): Either[Exception, FeedResult] = {
    debug(s"request::$request")
    val viewerId = request.viewerId.getOrElse(0L)
    // 未登录用户没有关注关系，直接返回空结果。
    if viewerId <= 0 then return Right(FeedResult(scene, List.empty, "", false))

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
      val param  = (viewerId, cursor, limit, () => listPageFromRepo(viewerId, cursorOpt, limit))
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
    // 关注流缓存键需要包含 viewerId，避免不同用户的关注页互相串数据。
    val cacheKey  = followingPageCacheKey(param.viewerId, param.cursor, param.limit)
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

  private def listPageFromRepo(viewerId: Long, cursor: Option[TimelineCursor], limit: Int): FeedPage = {
    // limit+1 是常见分页技巧：多取一条即可判断后面还有没有数据。
    val list = InboxRepository.listFollowingPage(viewerId, cursor, limit + 1)
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

  // 关注流缓存键需要绑定 viewerId。
  private def followingPageCacheKey(viewerId: Long, cursor: String, limit: Int): String = {
    val base = feedPageCacheKey(scene, cursor, limit)
    s"$base:viewer:$viewerId"
  }

  // 把排序字段编码成 URL 安全的游标字符串。
  private def encodeTimelineCursor(cursorOpt: Option[TimelineCursor]): String = {
    cursorOpt.filter(_.videoId > 0).map(encodeBase64).getOrElse("")
  }
}

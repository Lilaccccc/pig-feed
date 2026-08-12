package internal.feed.repository

import internal.feed.entity.*
import internal.feed.sql.*
import internal.interaction.enums.ActionType
import internal.video.enums.VideoStatus
import sqala.metadata.*
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime

@table("feed_inbox")
private final case class InboxModel(
  @autoInc id: Long,
  userId: Long,
  videoId: Long,
  authorId: Long,
  publishedAt: LocalDateTime,
  createdAt: LocalDateTime
)

object InboxRepository extends ColoredLogger {
  // 查询时间线 Feed 轻量页，卡片和计数由应用层批量组装。
  def listTimelinePage(cursor: Option[TimelineCursor], limit: Int = 10): List[FeedPageItem] = {
    val sql = listTimelinePageSql(cursor, limit)
    debug(s"listTimelinePageSql::$sql")
    try {
      db.fetchTo[FeedPageItem](sql)
    } catch {
      case e: Exception => error(e.getMessage); List.empty
    }
  }

  // 查询热榜 Feed 轻量页，按互动热度倒序稳定分页。
  def listHotPage(cursor: Option[HotCursor], limit: Int = 10): List[FeedPageItem] = {
    db.fetchTo[FeedPageItem](listHotPageSql(cursor, limit))
  }

  // 按关注关系读取关注流，作为 Redis 关注索引冷启动和缺失时的真相源兜底。
  def listFollowingPage(viewerId: Long, cursor: Option[TimelineCursor], limit: Int = 10): List[FeedPageItem] = {
    db.fetchTo[FeedPageItem](listFollowingPageSql(viewerId, cursor, limit))
  }

  // 查询当前用户关注的大 V 作者 ID，用于合并 Redis author outbox。
  def listFollowingPullAuthorIds(viewerId: Long): List[Long] = {
    db.fetchTo[Long](listFollowingPullAuthorIdsSql(viewerId))
  }

  // 批量读取视频卡片展示字段，缓存缺失时由应用层调用。
  def batchGetFeedCards(videoIds: List[Long]): Map[Long, FeedCard] = {
    if videoIds.isEmpty then return Map.empty
    db.fetchTo[FeedCard](batchGetFeedCardsSql(videoIds)).map(c => (c.videoId, c)).toMap
  }

  // 批量读取视频互动计数，缺失统计记录时按 0 处理。
  def batchGetFeedStats(videoIds: List[Long]): Map[Long, FeedStat] = {
    if videoIds.isEmpty then return Map.empty
    db.fetchTo[FeedStat](batchGetFeedStatsSql(videoIds)).map(s => (s.videoId, s)).toMap
  }

  // 批量读取当前用户对视频的点赞和收藏状态。
  def batchGetViewerActionStates(viewerId: Long, videoIds: List[Long]): Map[Long, ViewerActionState] = {
    if viewerId <= 0 || videoIds.isEmpty then return Map.empty

    val entity = (actionType: String, obj: ViewerActionState) =>
      actionType match
        case ActionType.Like.value     => obj.copy(liked = Some(true))
        case ActionType.Favorite.value => obj.copy(favorited = Some(true))
        case _                         => obj

    db.fetchTo[(videoId: Long, actionType: String)](batchGetViewerActionStatesSql(viewerId, videoIds))
      .foldLeft(Map.empty[Long, ViewerActionState]) { (acc, record) =>
        val (videoId, actionType) = record
        val existing              = acc.getOrElse(videoId, ViewerActionState(videoId))
        acc + (videoId -> entity(actionType, existing))
      }
  }

  // 查询作者最近公开视频，用于新关注小作者时回填当前用户 inbox。
  def listAuthorRecentVideos(authorId: Long, limit: Int): List[FeedPageItem] = {
    if authorId <= 0 || limit <= 0 then return List.empty[FeedPageItem]
    db.fetchTo[FeedPageItem](listAuthorRecentVideosSql(authorId, VideoStatus.Published, limit))
  }
}

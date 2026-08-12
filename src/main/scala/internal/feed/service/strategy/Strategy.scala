package internal.feed.service.strategy

import internal.feed.entity.*
import internal.feed.enums.FeedCache.*
import internal.feed.enums.Scene
import internal.feed.repository.InboxRepository
import internal.feed.service.cache.FeedCacheService
import internal.relation.repository.FollowRepository
import utils.base.ColoredLogger

// 定义单个 Feed 场景的读取策略。
trait Strategy extends ColoredLogger {
  def scene: Scene
  def list(request: FeedRequest): Either[Exception, FeedResult]

  def assembleFeedItems(viewerId: Option[Long], items: List[FeedPageItem]): List[FeedItem] = {
    debug(s"items::$items")
    val videoIds = items.filter(_.videoId > 0).map(_.videoId)
    debug(s"videoIds::$videoIds")
    if videoIds.isEmpty then return List.empty
    val videoIdSet = videoIds.toSet

    val cards = FeedCacheService.getCards(videoIds)
    debug(s"cards::$cards")
    val missingCardIds = videoIdSet.diff(cards.keySet)
    val loadCards      = if missingCardIds.nonEmpty then {
      val loadCards = InboxRepository.batchGetFeedCards(missingCardIds.toList)
      FeedCacheService.setCards(loadCards, FeedCardCacheTTL.value)
      loadCards
    } else Map.empty
    debug(s"loadCards::$loadCards")
    val cardMap = cards ++ loadCards

    val stats = FeedCacheService.getStats(videoIds)
    debug(s"stats::$stats")
    val missingStatIds = videoIdSet.diff(stats.keySet)
    val loadStats      = if missingStatIds.nonEmpty then {
      val loadStats = InboxRepository.batchGetFeedStats(missingStatIds.toList)
      FeedCacheService.setStats(loadStats, FeedStatCacheTTL.value)
      loadStats
    } else Map.empty
    debug(s"loadStats::$loadStats")
    val statMap = stats ++ loadStats
    debug(s"statMap::$statMap")

    val viewerActions = InboxRepository.batchGetViewerActionStates(viewerId.getOrElse(0L), videoIds)
    debug(s"viewerActions::$viewerActions")

    val authorIds = items.flatMap(_.authorId).distinct
    val followMap = viewerId match
      case Some(uid) if uid > 0 && authorIds.nonEmpty => FollowRepository.batchGetFollowStatus(uid, authorIds)
      case _                                          => Map.empty[Long, Boolean]
    debug(s"followMap::$followMap")

    val toMap = (item: FeedPageItem) => {
      val videoId = item.videoId
      val score   = item.hotScore.getOrElse(0)
      val card    = cardMap.getOrElse(videoId, FeedCard.empty(videoId))
      val stat    = statMap.getOrElse(videoId, FeedStat.empty(videoId))
      debug(s"stat::$stat")
      val publishedAt = if item.publishedAt.getOrElse(0) != 0 then item.publishedAt else if card != null then Some(card.publishedAt) else None
      val action      = viewerActions.get(videoId).map(a => (liked = a.liked, favorited = a.favorited)).getOrElse((liked = None, favorited = None))
      val following   = card.authorId match
        case aid if aid > 0 => Some(followMap.getOrElse(aid, false))
        case _              => None
      FeedItem.restoreFeedItem(
        videoId = videoId,
        authorId = card.authorId,
        authorNickname = card.authorNickname,
        authorAvatarUrl = Option(card.authorAvatarUrl).getOrElse(""),
        title = card.title,
        description = Option(card.description).getOrElse(""),
        mediaUrl = card.mediaUrl,
        coverUrl = card.coverUrl,
        likeCount = stat.likeCount,
        commentCount = stat.commentCount,
        favoriteCount = stat.favoriteCount,
        liked = action.liked,
        favorited = action.favorited,
        following = following,
        hotScore = score,
        publishedAt = publishedAt.orNull
      )
    }

    items.map(toMap)
  }
}

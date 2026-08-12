package internal.feed.service.cache

import internal.feed.entity.*
import internal.feed.enums.FeedCache.*
import internal.infra.errors.*
import internal.infra.utils.Time.rfc3339
import internal.infra.utils.clampCount
import internal.interaction.entity.*
import internal.interaction.enums.ActionType.Unknown
import internal.interaction.enums.{ActionStatus, ActionType}
import redis.clients.jedis.RedisClient
import redis.clients.jedis.exceptions.JedisDataException
import redis.clients.jedis.params.ZParams
import redis.clients.jedis.params.ZParams.Aggregate
import redis.clients.jedis.resps.Tuple
import utils.base.Converters.*
import utils.base.{ColoredLogger, decode}
import utils.redis.RedisOps

import java.time.{Instant, LocalDateTime, ZoneOffset}

// 定义 Feed 页、卡片和计数缓存能力。
object FeedCacheService extends ColoredLogger {
  private type ActionState = (userId: Long, videoId: Long, active: Boolean, actionType: ActionType, idempotencyKey: String, initialStat: VideoStat)

  // 读取缓存中的轻量 Feed 页。
  def getPage(key: String): Option[FeedPage] = RedisOps.get[FeedPage](key)

  // 写入轻量 Feed 页，并设置过期时间。
  def setPage(key: String, page: FeedPage, ttlSeconds: Long): Unit = RedisOps.set(key, page, ttlSeconds)

  // 批量读取视频卡片缓存。
  def getCards(videoIds: List[Long]): Map[Long, FeedCard] = RedisOps.mget[FeedCard](videoIds.cacheKeys(feedCardKey)).map(card => (card.videoId, card)).toMap

  // 批量写入视频卡片缓存。
  def setCards(cards: Map[Long, FeedCard], ttlSeconds: Long): Unit = RedisOps.mset[FeedCard](cards.map((k, v) => (feedCardKey(k), v)), ttlSeconds)

  // 批量读取视频计数缓存。
  def getStats(videoIds: List[Long]): Map[Long, FeedStat] = RedisOps.mget[FeedStat](videoIds.cacheKeys(feedStatKey)).map(stat => (stat.videoId, stat)).toMap

  // 批量写入视频计数缓存。
  def setStats(stats: Map[Long, FeedStat], ttlSeconds: Long): Unit = RedisOps.mset[FeedStat](stats.map((k, v) => (feedStatKey(k), v)), ttlSeconds)

  // 把一次互动热度写入 1 分钟粒度的热榜桶。
  def addHotScore(videoId: Long, scoreDelta: Long, at: Instant): Unit = if videoId > 0 && scoreDelta != 0 then {
      RedisOps.zIncrBy(hotMinuteKey(at), hotRankMember(videoId), scoreDelta.toDouble, HotMinuteBucketTTL.value)
  }

  // 合并最近 60 个分钟桶，返回一小时滑动窗口内的热榜页。
  def listHotWindowPage(windowEnd: Instant, offset: Int, limit: Int): List[FeedPageItem] = {
    if limit <= 0 then return List.empty
    val windowKey = hotWindowKey(windowEnd)
    if !RedisOps.exists(windowKey) then rebuildHotWindow(windowKey, windowEnd)

    val toResult = (tuple: Tuple) => {
      val videoId  = hotRankVideoID(tuple.getElement).getOrElse(0L)
      val hotScore = tuple.getScore.toInt
      FeedPageItem(videoId, hotScore = Some(hotScore))
    }

    RedisOps.client.zrangeWithScores(windowKey, if offset > 0 then offset else 0, offset + limit - 1).toScala.map(toResult).filter(_.videoId > 0)
  }

  // 重建热窗口缓存
  private def rebuildHotWindow(windowKey: String, windowEnd: Instant): Unit = {
    val pipe = RedisOps.client.pipelined()
    // 合并数据、清理无效数据、设置过期时间
    pipe.zunionstore(windowKey, ZParams().aggregate(Aggregate.SUM), hotWindowMinuteKeys(windowEnd)*)
    pipe.zremrangeByScore(windowKey, Double.MinValue, 0.toDouble)
    pipe.expire(windowKey, HotWindowCacheTTL.value)
    pipe.sync()
  }

  // 写入 Redis 行为状态和实时计数，供点赞收藏接口快速返回。
  def setActionState(state: ActionState): Either[Exception, ActionStateResult] = {
    val rc         = RedisOps.client
    val actionType = state.actionType
    if actionType == Unknown then return Left(ErrInvalidActionType())

    val videoId         = state.videoId
    val idempotencyKey  = state.idempotencyKey.trim
    val initialStat     = state.initialStat
    val actionKey       = interactionActionKey(state.userId, state.videoId, actionType)
    val counterBaseKey  = interactionStatCounterBaseKey(state.videoId)
    val counterShardKey = interactionStatCounterShardKey(state.videoId, interactionStatCounterShardIndex(state.userId))
    val jsonKey         = feedStatKey(state.videoId)
    val targetStatus    = if state.active then ActionStatus.Active else ActionStatus.Canceled

    val values = {
      val map = rc.hgetAll(actionKey).toScala
      if map.nonEmpty then map
      else Map("status" -> s"${ActionStatus.Canceled.value}")
    }
    val storedStatus         = ActionStatus.value(values.getOrElse("status", "0").toInt)
    val storedIdempotencyKey = values.getOrElse("idempotency_key", "")

    val toEffectiveActive = () => storedStatus == ActionStatus.Active

    val (effectiveActive, effectiveStatus, delta) = if storedIdempotencyKey.equals(state.idempotencyKey) && !idempotencyKey.isBlank then
      (effectiveActive = state.active, effectiveStatus = targetStatus, delta = 0)
    else {
      if storedStatus == ActionStatus.Canceled then {
        (effectiveActive = state.active, effectiveStatus = targetStatus, delta = if state.active then 1 else 0)
      } else if storedStatus != targetStatus then {
        (effectiveActive = state.active, effectiveStatus = targetStatus, delta = if state.active then 1 else -1)
      } else (effectiveActive = state.active, effectiveStatus = targetStatus, delta = 0)
    }

    val tx = rc.multi()

    tx.hset(actionKey, "status", effectiveStatus.value.toString)
    tx.hset(actionKey, "idempotency_key", idempotencyKey)
    tx.hset(actionKey, "updated_at", rfc3339.format(Instant.now()))
    tx.expire(actionKey, ActionStateTTL.value)

    // 初始化基础统计
    tx.hsetnx(counterBaseKey, "like_count", initialStat.likeCount.toString)
    tx.hsetnx(counterBaseKey, "comment_count", initialStat.commentCount.toString)
    tx.hsetnx(counterBaseKey, "favorite_count", initialStat.favoriteCount.toString)
    tx.expire(counterBaseKey, ActionStatTTL.value)

    // 增量更新计数
    if delta != 0 then tx.hincrBy(counterShardKey, actionType.cacheKey, delta)
    tx.expire(counterShardKey, ActionStatTTL.value)

    // 事务优化：若键被并发修改可以添加重试机制
    if tx.exec() == null then return Left(ErrPipeline())

    val stat = actionStatWithPresence(rc, counterBaseKey, interactionStatCounterShardKeys(videoId), jsonKey, videoId, initialStat)

    debug(s"actionType.value::${actionType.value}")
    debug(s"effectiveActive::$effectiveActive")
    Right(
      ActionStateResult(
        videoId = videoId,
        actionType = actionType.value,
        active = effectiveActive,
        delta = delta,
        idempotencyKey = idempotencyKey,
        likeCount = stat.likeCount,
        commentCount = stat.commentCount,
        favoriteCount = stat.favoriteCount
      )
    )
  }

  // 解决 Redis 热 Key 问题：基准 + 增量
  // 单 Key 存储：热门视频的统计 Key 被大量并发写入，成为热 Key
  // 分片存储（Shard）：将统计数据分散到多个 Shard Key，分散写入压力
  private def actionStatWithPresence(client: RedisClient, counterBaseKey: String, counterShardKeys: List[String], jsonKey: String, videoId: Long, initialStat: VideoStat) = {
    val values = client.hgetAll(counterBaseKey)
    val map    = if values == null then Map.empty[String, String] else values.toScala

    val (likeCount, commentCount, favoriteCount) = if map.nonEmpty then {
      (map.getOrElse("like_count", "0").toLong, map.getOrElse("comment_count", "0").toLong, map.getOrElse("favorite_count", "0").toLong)
    } else {
      val content = client.get(jsonKey)
      if content == null then {
        (initialStat.likeCount, initialStat.commentCount, initialStat.favoriteCount)
      } else content.decode[FeedStat].map(s => (s.likeCount, s.commentCount, s.favoriteCount)).getOrElse((0L, 0L, 0L))
    }

    val shard = if counterShardKeys.nonEmpty then {
      counterShardKeys.foldLeft((0L, 0L, 0L)) { (acc, key) =>
        val shardValues = client.hgetAll(key)
        if shardValues != null then {
          val shardMap = shardValues.toScala
          (
            likeCount = clampCount(acc._1 + shardMap.getOrElse("like_count", "0").toLong),
            commentCount = clampCount(acc._2 + shardMap.getOrElse("comment_count", "0").toLong),
            favoriteCount = clampCount(acc._3 + shardMap.getOrElse("favorite_count", "0").toLong)
          )
        } else acc
      }
    } else (likeCount = 0L, commentCount = 0L, favoriteCount = 0L)

    FeedStat(videoId, likeCount + shard.likeCount, commentCount + shard.commentCount, favoriteCount + shard.favoriteCount)
  }

  def setVideoStat(videoStat: VideoStat) = if videoStat.videoId > 0 then {
    val jsonKey  = feedStatKey(videoStat.videoId)
    val feedStat = FeedStat(videoStat.videoId, videoStat.likeCount, videoStat.commentCount, videoStat.favoriteCount)
    RedisOps.set(jsonKey, feedStat, ActionStatJSONTTL.value)
  }

  def addInboxItems(authorId: Long, userIds: List[Long], item: FeedPageItem, maxLen: Long): Option[ErrPipeline] = {
    val videoId = item.videoId
    if authorId <= 0L || item.videoId <= 0L || item.publishedAt.isEmpty || userIds.isEmpty then return None
    val maxLimit    = if maxLen <= 0L then 1000 else maxLen
    val publishedAt = item.publishedAt.get
    val score       = followingIndexScore(publishedAt, videoId)
    val member      = followingIndexMember(videoId, authorId, publishedAt)
    val tx          = RedisOps.client.multi()
    val _           = userIds.foreach(userId => {
      val key = followingInboxKey(userId)
      tx.zadd(key, score, member)
      tx.zremrangeByRank(key, 0, -maxLimit - 1)
      tx.expire(key, FollowingIndexKeyTTL.value)
    })
    val response     = tx.exec()
    val hasException = () => response.toScala.exists(_.isInstanceOf[JedisDataException])
    if response == null || hasException() then Some(ErrPipeline()) else None
  }

  private def followingIndexScore(publishedAt: LocalDateTime, videoId: Long): Double = {
    val epochSecond   = publishedAt.toEpochSecond(ZoneOffset.UTC)
    val lastSixDigits = Math.floorMod(videoId, 1_000_000L)
    epochSecond * 1_000_000L + lastSixDigits
  }

  private def followingIndexMember(videoId: Long, authorId: Long, publishedAt: LocalDateTime): String = s"$videoId:$authorId:${rfc3339.format(publishedAt)}"
  private def followingInboxKey(userId: Long)                                                         = s"feed:following:inbox:v1:$userId"
}

package internal.feed.enums

import com.google.common.hash.{HashCode, Hashing}
import internal.infra.enums.*
import internal.interaction.enums.ActionType
import org.apache.commons.codec.binary.Hex

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.MINUTES
import java.time.{Instant, ZoneOffset}
import scala.util.Try

// 秒
enum FeedCache(val value: Int) {
  case TimelineFirstPageCacheTTL extends FeedCache(5)
  case TimelinePageCacheTTL      extends FeedCache(45)
  case FeedCardCacheTTL          extends FeedCache(90)
  case FeedStatCacheTTL          extends FeedCache(15)

  case HotMinuteBucketTTL   extends FeedCache(7200)
  case HotWindowCacheTTL    extends FeedCache(120)
  case ActionStateTTL       extends FeedCache(2_592_000)
  case ActionStatTTL        extends FeedCache(86400)
  case ActionStatJSONTTL    extends FeedCache(15)
  case FollowingIndexKeyTTL extends FeedCache(2_592_000)
}

object FeedCache {
  extension (videoIds: List[Long]) {
    def cacheKeys(func: Long => String): List[String] = videoIds.map(func)
  }

  def feedCardKey(videoId: Long) = s"video:card:v1:$videoId"

  def feedStatKey(videoId: Long) = s"video:stat:v1:$videoId"

  def hotWindowKey(windowEnd: Instant): String = {
    val unix = windowEnd.truncatedTo(MINUTES).getEpochSecond
    s"feed:hot:window:v1:$unix"
  }

  def hotWindowMinuteKeys(windowEnd: Instant): List[String] = {
    val key = (index: Int) => hotMinuteKey(windowEnd.minusSeconds(index))
    (HotWindowMinutes to 0 by -1).map(key).toList
  }

  def hotMinuteKey(at: Instant): String = {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC)
    s"feed:hot:minute:v1:${formatter.format(at.truncatedTo(MINUTES))}"
  }

  def hotRankMember(videoId: Long): String = String.format("%020d", videoId)

  def hotRankVideoID(member: String): Option[Long] = Option(member)
    .map(_.dropWhile(_ == '0'))
    .filter(_.nonEmpty)
    .flatMap(value => Try(value.toLong).toOption)
    .filter(_ > 0)

  def feedPageCacheKey(scene: Scene, cursor: String, limit: Int): String = {
    if cursor.isBlank then s"feed:page:v1:${scene.value}:limit:$limit:first"
    else {
      val hashBytes = MessageDigest.getInstance("SHA-1").digest(cursor.getBytes(UTF_8))
      val hexStr    = Hex.encodeHexString(hashBytes)
      s"feed:page:v1:${scene.value}:limit:$limit:cursor:$hexStr"
    }
  }

  def feedPageCacheTtl(cursor: String, cacheKey: String, firstPageTtl: Long, pageTtl: Long): Long = {
    val ttl = if cursor.isBlank then firstPageTtl else pageTtl
    if ttl <= 0 then return 0
    // 为缓存过期时间引入一个随机抖动（Jitter），以防止缓存雪崩。
    val hash: HashCode = Hashing.murmur3_32_fixed().hashString(cacheKey, UTF_8)
    val jitterPercent  = 10 + Math.abs(hash.asInt) % 11
    ttl + (ttl * jitterPercent / 100)
  }

  def interactionActionKey(userId: Long, videoId: Long, actionType: ActionType): String = s"interaction:action:v1:$userId:$videoId:${actionType.value.toLowerCase}"

  def interactionStatCounterBaseKey(videoId: Long): String = s"${interactionStatCounterKey(videoId)}:base"

  def interactionStatCounterShardKeys(videoId: Long): List[String] = (0 until ActionStatCounterShardCount).map(shard => interactionStatCounterShardKey(videoId, shard)).toList

  def interactionStatCounterShardKey(videoId: Long, shard: Int): String = s"${interactionStatCounterKey(videoId)}:shard:${String.format("%020d", shard)}"

  def interactionStatCounterKey(videoId: Long): String = s"video:stat:counter:v1:$videoId"

  def interactionStatCounterShardIndex(userId: Long): Int = ((if userId <= 0 then 0 else userId) % ActionStatCounterShardCount).toInt
}

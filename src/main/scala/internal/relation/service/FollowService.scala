package internal.relation.service

import internal.feed.enums.BigCreatorFollowerThreshold
import internal.feed.repository.InboxRepository
import internal.feed.service.cache.FeedCacheService
import internal.feed.utils.decodeCursor
import internal.infra.enums.*
import internal.infra.errors.*
import internal.message.enums.MessageType
import internal.message.service.MessageService
import internal.relation.entity.*
import internal.relation.enums.*
import internal.relation.repository.*
import utils.base.ColoredLogger

// 编排用户关系用例：关注、取关、关注列表和粉丝列表。
object FollowService extends ColoredLogger {
  // 设置当前用户(取消)关注目标用户。
  def follow(userId: Long, targetUserId: Long, active: Boolean, idempotencyKey: String): Either[Exception, FollowResult] = {
    if userId <= 0 then return Left(ErrInvalidUserID())

    if targetUserId <= 0 then return Left(ErrInvalidTargetUserID())
    if userId == targetUserId then return Left(ErrFollowSelfForbidden())

    val idempotencyKeyTrim = idempotencyKey.trim
    if idempotencyKeyTrim.length > MaxIdempotencyKeyLength then return Left(ErrIdempotencyKeyTooLong())

    def backfillFollowFeed(userId: Long, targetUserId: Long): Option[ErrBackfillFollowFeedFailed] = {
      val followerCount = RelationStatRepository.countFollowers(targetUserId)
      if followerCount >= BigCreatorFollowerThreshold then return None
      val items        = InboxRepository.listAuthorRecentVideos(targetUserId, DefaultBackfillVideoLimit)
      val hasException = items.map(item => FeedCacheService.addInboxItems(targetUserId, List(userId), item, DefaultBackfillInboxMaxLen)).exists(_.nonEmpty)
      if hasException then Some(ErrBackfillFollowFeedFailed()) else None
    }

    def notifyFollow(userId: Long, targetUserId: Long) = {
      val eventId = s"relation:follow:$targetUserId:$userId"
      FollowRepository
        .getUserProfile(userId)
        .map(actor => MessageService.createFromActorEvent(targetUserId, MessageType.Follow, "新增关注", s"${actor.nickname} 关注了你", eventId, eventId, userId, actor.nickname, actor.avatarUrl))
        .getOrElse(MessageService.createFromActorEvent(targetUserId, MessageType.Follow, "新增关注", "关注了你", eventId, eventId, userId, "", ""))
    }

    val result = (setFollowResult: (follow: Follow, userStat: RelationStat, targetStat: RelationStat)) => {
      val follow     = setFollowResult.follow
      val userStat   = setFollowResult.userStat
      val targetStat = setFollowResult.targetStat
      val body       = () => FollowResult(follow.userId, follow.targetUserId, follow.status, follow.active, userStat.followingCount, targetStat.followerCount)

      if active && follow.active then
        backfillFollowFeed(userId, targetUserId)
          .map(err => Left(err))
          .getOrElse {
            notifyFollow(userId, targetUserId).fold(_ => warn(s"通知关注消息出错：消息已存在"), _ => ())
            Right(body())
          }
      else Right(body())
    }

    FollowRepository.setFollow(userId, targetUserId, active, idempotencyKey).fold(err => Left(err), result)
  }

  // 查询当前用户的关注列表。
  def listFollowing(userId: Long, cursor: String, limit: Int): Either[Exception, ListResult] = {
    if userId <= 0L then return Left(ErrInvalidUserID())
    parseListCursor(cursor).fold(
      err => Left(err),
      parsedCursor => {
        val normalizeLimit = limit.normalizeLimit
        val items          = FollowRepository.listFollowing(userId, parsedCursor, normalizeLimit + 1)
        Right(listResult(items, normalizeLimit))
      }
    )
  }

  // 查询当前用户的粉丝列表。
  def listFollowers(userId: Long, cursor: String, limit: Int): Either[Exception, ListResult] = {
    if userId <= 0L then return Left(ErrInvalidUserID())
    parseListCursor(cursor).fold(
      err => Left(err),
      parsedCursor => {
        val normalizeLimit = limit.normalizeLimit
        val items          = FollowRepository.listFollowers(userId, parsedCursor, normalizeLimit + 1)
        Right(listResult(items, normalizeLimit))
      }
    )
  }

  private def parseListCursor(raw: String): Either[Exception, Option[ListCursor]] = {
    if raw.isBlank then return Right(None)
    val result = (cursor: ListCursor) => if cursor.userId <= 0 then Left(ErrInvalidCursor()) else Right(Some(cursor))
    decodeCursor(raw)
      .flatMap(bytes => ListCursor.decodeBytes(bytes))
      .fold(err => Left(err), result)
  }

  private def listResult(items: List[UserItem], limit: Int): ListResult = {
    val hasMore    = items.size > limit
    val list       = if hasMore then items.slice(0, limit) else items
    val nextCursor = if list.nonEmpty then {
      val last = list.last
      ListCursor(last.followedAt, last.userId).encodeBase64
    } else ""
    ListResult(list, nextCursor, hasMore)
  }
}

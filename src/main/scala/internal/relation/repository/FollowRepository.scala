package internal.relation.repository

import internal.auth.enums.account.Status
import internal.auth.repository.UserRepository
import internal.infra.utils.clampCount
import internal.relation.entity.*
import internal.relation.enums.FollowStatus
import internal.relation.sql.*
import sqala.jdbc.JdbcTransactionContext
import sqala.metadata.*
import utils.base.ColoredLogger
import utils.db.{db, sql}

import java.time.LocalDateTime
import scala.util.Try

// 映射 user_follow 表，记录用户之间的关注状态。
@table("user_follow")
final case class FollowModel(
  @autoInc id: Long,
  userId: Long,
  targetUserId: Long,
  status: Int,
  idempotencyKey: Option[String],
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime]
) {
  def restoreFollow = Follow(
    id,
    userId,
    targetUserId,
    status,
    idempotencyKey.map(_.trim).getOrElse(""),
    createdAt.orNull,
    updatedAt.orNull
  )
}

object FollowRepository extends ColoredLogger {
  private type SetFollowResult = (follow: Follow, userStat: RelationStat, targetStat: RelationStat)

  // 读取用户展示资料，用于关注通知。
  def getUserProfile(userId: Long): Option[UserProfile] = {
    val sql = getUserProfileSql(userId, Status.Normal.value)
    db.fetchTo[UserProfile](sql).headOption
  }

  // 查询当前用户关注的人。
  def listFollowing(userId: Long, cursor: Option[ListCursor], limit: Int): List[UserItem] = {
    val sql = listFollowingSql(userId, cursor, FollowStatus.Active.value, Status.Normal.value, limit)
    db.fetchTo[UserItem](sql)
  }

  // 查询关注当前用户的人。
  def listFollowers(userId: Long, cursor: Option[ListCursor], limit: Int): List[UserItem] = {
    val sql = listFollowersSql(userId, cursor, FollowStatus.Active.value, Status.Normal.value, limit)
    db.fetchTo[UserItem](sql)
  }

  // 设置关注或取关状态，并在同一事务中维护双方计数。
  def setFollow(userId: Long, targetUserId: Long, active: Boolean, idempotencyKey: String): Either[Exception, SetFollowResult] = {
    val nextStatus         = FollowStatus.value(active)
    val idempotencyKeyTrim = idempotencyKey.trim

    val doing = () =>
      db.executeTransaction {
        UserRepository.lockNormalUser(userId)
        UserRepository.lockNormalUser(targetUserId)

        val notFound = () => {
          val key    = if idempotencyKeyTrim.isBlank then None else Some(idempotencyKeyTrim)
          val now    = LocalDateTime.now
          val follow = FollowModel(0, userId, targetUserId, nextStatus.value, key, Some(now), Some(now))
          db.insert(follow)
          val delta      = if active then 1 else 0
          val userStat   = updateStat(userId, delta, 0)
          val targetStat = updateStat(targetUserId, 0, delta)
          (follow = follow.restoreFollow, userStat = userStat.restoreStat, targetStat = targetStat.restoreStat)
        }

        val found = (follow: FollowModel) => {
          val previousStatus         = follow.status
          val previousIdempotencyKey = follow.idempotencyKey.map(_.trim).getOrElse("")
          val delta                  = if follow.status != nextStatus.value then if active then 1 else -1 else 0
          val model = follow.copy(status = nextStatus.value, idempotencyKey = if idempotencyKeyTrim.isBlank then None else Some(idempotencyKeyTrim), updatedAt = Some(LocalDateTime.now))
          if previousStatus != nextStatus.value && !previousIdempotencyKey.equals(idempotencyKeyTrim) then {
            db.update(model, true)
            val userStat = updateStat(userId, delta, 0)
            val targetStat = updateStat(targetUserId, 0, delta)
            (follow = model.restoreFollow, userStat = userStat.restoreStat, targetStat = targetStat.restoreStat)
          } else (follow = model.restoreFollow, userStat = currentStat(userId).map(_.restoreStat).orNull, targetStat = currentStat(targetUserId).map(_.restoreStat).orNull)
        }

        val result = findFollow(userId, targetUserId).map(found).getOrElse(notFound())
        (follow = result.follow, userStat = result.userStat, targetStat = result.targetStat)
      }

    Try(doing()).fold(
      {
        case ex: Exception => Left(ex)
        case other         => Left(RuntimeException(other))
      },
      result => Right(result)
    )
  }

  private def findFollow(userId: Long, targetUserId: Long): Option[FollowModel] = {
    import sqala.static.dsl.*
    val dsl = from(FollowModel).filter(f => f.userId == userId && f.targetUserId == targetUserId)
    val sql = query(dsl.forUpdate)
    db.fetch(sql).headOption
  }

  private def updateStat(userId: Long, followingDelta: Int, followerDelta: Int)(using JdbcTransactionContext): RelationStatModel = {
    currentStat(userId).map(stat =>
        val followingCount = clampCount(stat.followingCount + followingDelta)
        val followerCount = clampCount(stat.followerCount + followerDelta)
        val now = LocalDateTime.now
        val model = stat.copy(followingCount = followingCount,followerCount = followerCount, updatedAt = Some(now))
        import sqala.static.dsl.*
        val dsl = update[RelationStatModel]
          .set(_.followingCount := followingCount)
          .set(_.followerCount := followerCount)
          .set(_.updatedAt := now)
          .where(_.userId == userId)
        db.execute(dsl)
        model
      )
      .getOrElse {
        val now   = LocalDateTime.now
        val model = RelationStatModel(userId, clampCount(0 + followingDelta), clampCount(0 + followerDelta), Some(now), Some(now))
        db.insert(model)
        model
      }
  }

  private def currentStat(userId: Long): Option[RelationStatModel] = {
    import sqala.static.dsl.*
    val dsl = from(RelationStatModel).filter(r => r.userId == userId)
    val sql = query(dsl)
    db.fetch(sql).headOption
  }

  def listFollowerIds(authorId: Long, cursor: Long, limit: Int): List[Long] = {
    import sqala.static.dsl.*
    val dsl = from(FollowModel)
      .filter(f => f.targetUserId == authorId && f.status == FollowStatus.Active.value)
      .filterIf(cursor > 0L)(_.userId > cursor)
      .select(_.userId)
      .sortBy(_.userId.asc)
      .take(limit)
    val sql = query(dsl)
    db.fetch(sql)
  }

  def batchGetFollowStatus(userId: Long, targetUserIds: List[Long]): Map[Long, Boolean] = {
    if targetUserIds.isEmpty then return Map.empty
    val ids = targetUserIds.mkString(",")
    val sql = s"SELECT target_user_id FROM user_follow WHERE user_id = $userId AND target_user_id IN ($ids) AND status = ${FollowStatus.Active.value}".sql
    db.fetchTo[Long](sql).map(id => (id, true)).toMap
  }
}

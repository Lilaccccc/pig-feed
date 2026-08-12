package internal.relation.sql

import internal.relation.entity.ListCursor
import utils.db.sql

import java.time.Instant

def getUserProfileSql(id: Long, status: Int) = s"SELECT id AS user_id, nickname, avatar_url, bio FROM account WHERE id = $id AND status = $status".sql

def listFollowingSql(userId: Long, cursor: Option[ListCursor], userFollowStatus: Int, accountStatus: Int, limit: Int) = {
  val cond = cursor match {
    case Some(value) =>
      val followedAt     = value.followedAt
      val followedUserId = value.userId
      s"AND (f.updated_at < $followedAt OR (f.updated_at = $followedAt AND f.target_user_id < $followedUserId))"
    case None => ""
  }

  s"""SELECT a.id AS user_id, a.nickname, a.avatar_url, a.bio, f.updated_at AS followed_at FROM user_follow AS f
     |LEFT JOIN account AS a ON a.id = f.target_user_id
     |WHERE f.user_id = $userId AND f.status = $userFollowStatus AND a.status = $accountStatus
     |$cond ORDER BY f.updated_at DESC, f.target_user_id DESC LIMIT $limit
     |""".stripMargin.sql
}

def listFollowersSql(userId: Long, cursor: Option[ListCursor], userFollowStatus: Int, accountStatus: Int, limit: Int) = {
  val cond = cursor match {
    case Some(value) =>
      val followedAt     = value.followedAt
      val followedUserId = value.userId
      s"AND (f.updated_at < $followedAt OR (f.updated_at = $followedAt AND f.user_id < $followedUserId))"
    case None => ""
  }

  s"""SELECT a.id AS user_id, a.nickname, a.avatar_url, a.bio, f.updated_at AS followed_at FROM user_follow AS f
     |LEFT JOIN account AS a ON a.id = f.user_id
     |WHERE f.target_user_id = $userId AND f.status = $userFollowStatus AND a.status = $accountStatus
     |$cond ORDER BY f.updated_at DESC, f.user_id DESC LIMIT $limit
     |""".stripMargin.sql
}

def ensureStatSql(userId: Long) = {
  val now = Instant.now.getEpochSecond
  s"""
     |INSERT IGNORE INTO `user_relation_stat` (`user_id`, `created_at`, `updated_at`)
     |VALUES ($userId, $now, $now)""".stripMargin.sql
}

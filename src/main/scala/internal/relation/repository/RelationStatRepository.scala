package internal.relation.repository

import internal.relation.entity.RelationStat
import sqala.metadata.*
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime

// 映射 user_relation_stat 表，保存关注数和粉丝数。
@table("user_relation_stat")
final case class RelationStatModel(
  userId: Long,
  followingCount: Int,
  followerCount: Int,
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime]
) {
  def restoreStat = RelationStat(
    userId,
    followingCount,
    followerCount,
    createdAt.orNull,
    updatedAt.orNull
  )
}

object RelationStatRepository extends ColoredLogger {
  def countFollowers(authorId: Long): Int = {
    import sqala.static.dsl.*
    val dsl = from(RelationStatModel).filter(_.userId == authorId).select(_.followerCount)
    val sql = query(dsl)
    db.fetch(sql).headOption.getOrElse(0)
  }
}

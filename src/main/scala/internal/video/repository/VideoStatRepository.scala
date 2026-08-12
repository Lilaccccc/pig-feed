package internal.video.repository

import internal.auth.enums.account.Role
import internal.auth.enums.account.Status.Normal
import internal.interaction.entity.*
import internal.interaction.enums.CommentStatus
import internal.video.enums.VideoStatus.Published
import internal.video.sql.*
import sqala.metadata.{autoInc, table}
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime

// 映射 video_stat 表，保存可频繁变更的互动计数。
@table("video_stat")
final case class VideoStatModel(
  @autoInc videoId: Long,
  likeCount: Long,
  commentCount: Long,
  favoriteCount: Long,
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime]
)

object VideoStatRepository extends ColoredLogger {
  // 读取公开视频当前互动计数。
  def getVideoStat(videoId: Long): Option[VideoStat] = db.fetchTo[VideoStat](getVideoStatSql(videoId, Published)).headOption

  // 读取公开视频作者 ID，用于互动消息通知。
  def getVideoAuthorId(videoId: Long): Option[Long] = {
    import sqala.static.dsl.*
    val sql = query {
      from(VideoModel)
        .filter(_.id == videoId)
        .filter(_.status == Published.value)
        .select(_.authorId)
    }
    db.fetch(sql).headOption
  }

  // 读取用户展示资料，用于互动消息展示触发者。
  def getUserProfile(userId: Long): Option[UserProfile] = db.fetchTo[UserProfile](getUserProfileSql(userId, Normal)).headOption
}

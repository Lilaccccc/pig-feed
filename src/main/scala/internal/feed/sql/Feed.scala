package internal.feed.sql

import internal.feed.entity.{HotCursor, TimelineCursor}
import internal.feed.enums.BigCreatorFollowerThreshold
import internal.interaction.enums.*
import internal.relation.enums.FollowStatus.*
import internal.video.enums.VideoStatus
import internal.video.enums.VideoStatus.*
import utils.db.sql

def listTimelinePageSql(cursor: Option[TimelineCursor], limit: Int) = {
  val cond = if cursor.isEmpty then ""
  else {
    val publishedAt = cursor.get.publishedAt
    val videoId     = cursor.get.videoId
    s"AND (v.published_at < $publishedAt OR (v.published_at = $publishedAt AND v.id < $videoId))"
  }

  s"""SELECT
     |    v.id AS video_id,
     |    v.author_id,
     |    v.published_at,
     |    0 AS hotScore
     |FROM
     |    video AS v
     |WHERE
     |    v.status = ${Published.value}
     |    AND v.published_at IS NOT NULL
     |    $cond
     |ORDER BY
     |    v.published_at DESC,
     |    v.id DESC
     |LIMIT $limit""".stripMargin.sql
}

def hotScoreExpression = "COALESCE(vs.like_count, 0) * 3 + COALESCE(vs.comment_count, 0) * 5 + COALESCE(vs.favorite_count, 0) * 4"

def listHotPageSql(cursor: Option[HotCursor], limit: Int) = {
  val cond = if cursor.isEmpty then ""
  else {
    val hotScore    = cursor.get.hotScore
    val publishedAt = cursor.get.publishedAt
    val videoId     = cursor.get.videoId
    s"AND (($hotScoreExpression) < $hotScore OR (($hotScoreExpression) = $hotScore AND v.published_at < $publishedAt) OR (($hotScoreExpression) = $hotScore AND v.published_at = $publishedAt AND v.id < $videoId))"
  }

  s"""SELECT
     |    v.id AS video_id,
     |    v.author_id,
     |    v.published_at,
     |    ($hotScoreExpression) AS hot_score
     |FROM
     |    video AS v
     |LEFT JOIN
     |    video_stat AS vs ON vs.video_id = v.id
     |WHERE
     |    v.status = ${Published.value}
     |    AND v.published_at IS NOT NULL
     |    $cond
     |ORDER BY
     |    hot_score DESC,
     |    v.published_at DESC,
     |    v.id DESC
     |LIMIT $limit""".stripMargin.sql
}

def listFollowingPageSql(viewerId: Long, cursor: Option[TimelineCursor], limit: Int = 10) = {
  val cond = if cursor.isEmpty then ""
  else {
    val publishedAt = cursor.get.publishedAt
    val videoId     = cursor.get.videoId
    s"AND (v.published_at < $publishedAt OR (v.published_at = $publishedAt AND v.id < $videoId))"
  }

  s"""SELECT
     |    v.id AS video_id,
     |    v.author_id,
     |    v.published_at,
     |    0 AS hotScore
     |FROM
     |    video AS v
     |JOIN
     |    user_follow AS f ON f.target_user_id = v.author_id
     |WHERE
     |    f.user_id = $viewerId
     |    AND f.status = ${Active.value}
     |    AND v.status = ${Published.value}
     |    AND v.published_at IS NOT NULL
     |    $cond
     |ORDER BY
     |    v.published_at DESC,
     |    v.id DESC
     |LIMIT $limit""".stripMargin.sql
}

def listFollowingPullAuthorIdsSql(viewerId: Long) = {
  s"""SELECT
     |    f.target_user_id
     |FROM
     |    user_follow AS f
     |JOIN
     |    user_relation_stat AS rs ON rs.user_id = f.target_user_id
     |WHERE
     |    f.user_id = $viewerId
     |    AND f.status = ${Active.value}
     |    AND rs.follower_count >= $BigCreatorFollowerThreshold
     |ORDER BY
     |    f.target_user_id ASC""".stripMargin.sql
}

def batchGetFeedCardsSql(videoIds: List[Long]) = {
  val videoIdStr = videoIds.mkString("(", ", ", ")")

  s"""SELECT
     |    v.id AS video_id,
     |    v.author_id,
     |    a.nickname AS author_nickname,
     |    a.avatar_url AS author_avatar_url,
     |    v.title,
     |    v.description,
     |    v.media_url,
     |    v.cover_url,
     |    v.published_at
     |FROM
     |    video AS v
     |LEFT JOIN
     |    account AS a ON a.id = v.author_id
     |WHERE
     |    v.id IN $videoIdStr
     |    AND v.status = ${Published.value}
     |    AND v.published_at IS NOT NULL""".stripMargin.sql
}

def batchGetFeedStatsSql(videoIds: List[Long]) = {
  val videoIdStr = videoIds.mkString("(", ", ", ")")

  s"""SELECT
     |    video_id,
     |    like_count,
     |    comment_count,
     |    favorite_count
     |FROM
     |    video_stat
     |WHERE
     |    video_id IN $videoIdStr""".stripMargin.sql
}

def batchGetViewerActionStatesSql(viewerId: Long, videoIds: List[Long]) = {
  val videoIdStr    = videoIds.mkString("(", ", ", ")")
  val actionTypeStr = s"('${ActionType.Like.value}', '${ActionType.Favorite.value}')"

  s"""SELECT
     |    video_id,
     |    action_type
     |FROM
     |    interaction_action
     |WHERE
     |    user_id = $viewerId
     |    AND video_id IN $videoIdStr
     |    AND status = ${ActionStatus.Active.value}
     |    AND action_type IN $actionTypeStr""".stripMargin.sql
}

def listAuthorRecentVideosSql(authorId: Long, status: VideoStatus, limit: Int) = {
  s"""SELECT v.id AS video_id, v.author_id, v.published_at, 0 AS hot_score
     |FROM video AS v
     |WHERE v.author_id = $authorId AND v.status = ${status.value} AND v.published_at IS NOT NULL
     |ORDER BY v.published_at DESC, v.id DESC
     |LIMIT $limit
     |""".stripMargin.sql
}

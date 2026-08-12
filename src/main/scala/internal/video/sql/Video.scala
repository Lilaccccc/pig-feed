package internal.video.sql

import internal.video.enums.VideoStatus
import utils.db.sql

def ensureStatsSql = {
  """INSERT INTO video_stat (video_id, like_count, comment_count, favorite_count, created_at, updated_at)
    |SELECT v.id, 0, 0, 0, NOW(), NOW()
    |FROM video AS v
    |LEFT JOIN video_stat AS vs ON vs.video_id = v.id
    |WHERE vs.video_id IS NULL;
    |""".stripMargin.sql
}

private def findVideoBase = {
  """SELECT
    |    v.id,
    |    v.author_id,
    |    v.title,
    |    v.description,
    |    v.media_url,
    |    v.cover_url,
    |    v.status,
    |    COALESCE(vs.like_count, 0) AS like_count,
    |    COALESCE(vs.comment_count, 0) AS comment_count,
    |    COALESCE(vs.favorite_count, 0) AS favorite_count,
    |    v.published_at,
    |    v.idempotency_key,
    |    v.created_at,
    |    v.updated_at
    |FROM
    |    video AS v
    |LEFT JOIN
    |    video_stat AS vs ON vs.video_id = v.id""".stripMargin
}

def findByVideoSql(id: Long, status: Option[VideoStatus] = None) = {
  val cond = if status.isEmpty then "" else s"AND v.status = ${status.get.value}"
  s"$findVideoBase WHERE v.id = $id $cond LIMIT 1;".sql
}

def findByAuthorAndIdempotencyKeySql(authorId: Long, key: String) = s"$findVideoBase WHERE v.author_id = $authorId AND v.idempotency_key = '$key' LIMIT 1;".sql

def findListByAuthorSql(authorId: Long, status: VideoStatus, limit: Int, offset: Int) =
  s"$findVideoBase WHERE v.author_id = $authorId AND v.status = ${status.value} ORDER BY v.published_at DESC, v.id DESC LIMIT $limit OFFSET $offset;".sql

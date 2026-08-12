package internal.interaction.sql

import internal.interaction.entity.CommentCursor
import internal.interaction.enums.CommentStatus
import utils.db.sql

private def commentWithUserSelect =
  "c.id, c.video_id, c.user_id, a.nickname AS user_nickname, a.avatar_url AS user_avatar_url, c.content, c.status, IFNULL(c.idempotency_key, '') AS idempotency_key, c.created_at, c.updated_at"

def findCommentByUserAndIdempotencyKeySql(userId: Long, idempotencyKey: String) =
  s"""SELECT
     |  $commentWithUserSelect
     |FROM interaction_comment AS c
     |LEFT JOIN account AS a ON a.id = c.user_id
     |WHERE c.user_id = $userId AND c.idempotency_key = '$idempotencyKey'
     |LIMIT 1
     |""".stripMargin.sql

def findCommentByIdSql(commentId: Long) =
  s"""SELECT
     |  $commentWithUserSelect
     |FROM interaction_comment AS c
     |LEFT JOIN account AS a ON a.id = c.user_id
     |WHERE c.id = $commentId
     |""".stripMargin.sql

def listCommentsSql(videoId: Long, cursor: Option[CommentCursor], limit: Int, status: CommentStatus) = {
  val cond = cursor.map(c => s"AND (c.created_at < ${c.createdAt} OR (c.created_at = ${c.createdAt} AND c.id < ${c.commentId}))").getOrElse("")
  s"""SELECT
     |  $commentWithUserSelect  
     |FROM interaction_comment AS c
     |LEFT JOIN account AS a ON a.id = c.user_id
     |WHERE c.video_id = $videoId AND c.status = ${status.value}
     |$cond
     |ORDER BY c.created_at DESC, c.id DESC
     |LIMIT $limit
     |""".stripMargin.sql
}

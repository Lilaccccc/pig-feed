package internal.video.sql

import internal.auth.enums.account.Status
import internal.video.enums.VideoStatus
import utils.db.sql

def getVideoStatSql(videoId: Long, status: VideoStatus) =
  s"""SELECT 
     |  vs.video_id, vs.like_count, 
     |  vs.comment_count, vs.favorite_count
     |FROM video_stat AS vs
     |JOIN video AS v ON v.id = vs.video_id
     |WHERE vs.video_id = $videoId AND v.status = ${status.value}
     |""".stripMargin.sql

def getUserProfileSql(userId: Long, status: Status) =
  s"""SELECT
     |  id, nickname, avatar_url
     |FROM account
     |WHERE id = $userId AND status = ${status.value}
     |""".stripMargin.sql

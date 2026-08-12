package internal.exposure.sql

import internal.exposure.enums.RecentExposureWindow
import internal.exposure.repository.ExposureModel
import internal.feed.sql.hotScoreExpression
import internal.infra.utils.Time.*
import internal.video.enums.VideoStatus.Published
import utils.db.sql

import java.time.LocalDateTime

def listCandidatePoolSql(userId: Long, limit: Int) =
  s"""SELECT v.id AS video_id, v.author_id, 0 AS rank_score, 0 AS similarity, ($hotScoreExpression) AS hot_score, 0 AS freshness_score, "" AS reason, v.published_at
     |FROM video AS v LEFT JOIN video_stat AS vs ON vs.video_id = v.id
     |LEFT JOIN exposures AS e ON e.user_id = $userId AND e.video_id = v.id AND e.last_exposed_at >= '${LocalDateTime.now.plusSeconds(RecentExposureWindow)}'
     |WHERE v.status = ${Published.value} AND v.published_at IS NOT NULL AND e.video_id IS NULL
     |ORDER BY hot_score DESC, v.published_at DESC, v.id DESC
     |LIMIT $limit
     |""".stripMargin.sql

def saveExposuresSql(exposures: List[ExposureModel]) = {
  val insertData = exposures
    .map(e =>
      s"""(${e.userId}, ${e.videoId}, '${rfc3339WithNotZ.format(e.firstExposedAt)}', '${rfc3339WithNotZ.format(e.lastExposedAt)}', ${e.exposureCount}, '${e.lastScene}', '${rfc3339WithNotZ.format(
          e.createdAt.get
        )}','${rfc3339WithNotZ.format(e.updatedAt.get)}')"""
    )
    .mkString(", ")

  s"""INSERT INTO `exposures`
     |    (`user_id`, `video_id`, `first_exposed_at`, `last_exposed_at`, `exposure_count`, `last_scene`, `created_at`, `updated_at`)
     |VALUES $insertData
     |ON DUPLICATE KEY UPDATE
     |    `last_exposed_at` = VALUES(`last_exposed_at`),
     |    `exposure_count` = `exposure_count` + 1,
     |    `last_scene` = VALUES(`last_scene`),
     |    `updated_at` = VALUES(`updated_at`);""".stripMargin.sql
}

package internal.exposure.entity

import java.time.LocalDateTime

// 保存用户看过某个视频的聚合事实，供推荐系统在线去重查询
final case class Exposure(
  id: Long,
  userId: Long,
  videoId: Long,
  firstExposedAt: LocalDateTime,
  lastExposedAt: LocalDateTime,
  exposureCount: Int,
  lastScene: String,
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime]
)

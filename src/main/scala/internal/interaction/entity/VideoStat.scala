package internal.interaction.entity

import io.circe.Codec

import java.time.LocalDateTime

// 保存互动模块需要的视频统计快照。
final case class VideoStat(
  videoId: Long,
  likeCount: Long,
  commentCount: Long,
  favoriteCount: Long
) derives Codec

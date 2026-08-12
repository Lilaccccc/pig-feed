package internal.video.controller.dto

import internal.video.controller.dto.VideoResponse.videoResponseFromDomain
import internal.video.entity.Video
import io.circe.Codec
import sttp.tapir.Schema

// offset 分页列表响应。
case class VideoListResponse(
  items: List[VideoResponse],
  limit: Int,
  offset: Int
) derives Codec, Schema

object VideoListResponse {
  extension (vList: List[Video]) {
    def videoListResponseFromDomain(limit: Int, offset: Int) = VideoListResponse(
      vList.map(videoResponseFromDomain),
      limit,
      offset
    )
  }
}

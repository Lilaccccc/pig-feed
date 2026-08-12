package internal.video.controller.handler

import internal.video.controller.dto.CreateVideoRequest
import internal.video.controller.dto.VideoListResponse.videoListResponseFromDomain
import internal.video.controller.dto.VideoResponse.videoResponseFromDomain
import internal.video.service.VideoService
import utils.base.ColoredLogger
import utils.result.throws

object UsersVideoHandler extends ColoredLogger {
  // 处理发布视频请求，用户身份来自 JWT，上行数据来自 JSON 请求体。
  // Idempotency-Key 来自请求头，用于客户端重试时获得同一个视频结果。
  def create(idempotencyKey: String, userId: Long, request: CreateVideoRequest) = {
    VideoService
      .createPublished(userId, request.title, request.description, request.mediaUrl, request.coverUrl, idempotencyKey)
      .fold(err => err.throws(using this), response => response.video.videoResponseFromDomain)
  }

  // 查询公开视频详情，videoId 来自 RESTful 路径参数。
  def get(videoId: Long) = VideoService.get(videoId).fold(err => err.throws(using this), response => response.videoResponseFromDomain)

  // 删除当前用户自己的视频，删除操作在领域层做作者权限校验。
  def delete(userId: Long, videoId: Long) = VideoService.delete(userId, videoId).fold(err => err.throws(using this), response => videoId)

  // 查询指定用户的公开作品列表。
  def listByAuthor(userId: Long, limit: Int, offset: Int) = VideoService
    .listByAuthor(userId, limit, offset)
    .fold(err => err.throws(using this), response => response.videoListResponseFromDomain(limit, offset))
}

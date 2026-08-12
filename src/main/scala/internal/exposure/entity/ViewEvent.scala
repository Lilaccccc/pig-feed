package internal.exposure.entity

import internal.exposure.enums.EventType
import internal.exposure.enums.EventType.Exposed

import java.time.LocalDateTime

// 保存一次客户端观看行为，适合做行为流水和后续推荐特征
final case class ViewEvent(
  id: Long,
  userId: Long,
  videoId: Long,
  scene: String,
  requestId: String,
  eventType: EventType,
  watchMs: Int,
  completed: Boolean,
  createdAt: Option[LocalDateTime] = Some(LocalDateTime.now)
) {
  // 判断当前事件是否写入曝光聚合索引
  def countsAsExposure: Boolean = this.eventType == Exposed

  def requestIdPtr: Option[String] = {
    val requestId = this.requestId.trim
    if requestId.isBlank then None else Some(requestId)
  }
}

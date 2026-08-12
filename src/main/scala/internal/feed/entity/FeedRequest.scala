package internal.feed.entity

import internal.feed.enums.*
import internal.feed.enums.Scene.*
import internal.feed.service.strategy.*
import internal.infra.enums.MaxLimit

// 所有 Feed 场景共用的查询参数。
final case class FeedRequest(
  scene: String,
  cursor: String,
  limit: Int,
  viewerId: Option[Long] = None,
  clientContext: Map[String, String] = Map.empty
) {
  private def normalizeScene: Scene = Scene.value(scene)

  lazy val strategy: Strategy = this.normalizeScene match {
    case Timeline  => TimelineStrategy
    case Recommend => RecommendStrategy
    case Following => FollowingStrategy
    case Hot       => HotStrategy
    case _         => TimelineStrategy
  }

  lazy val normalizeLimit: Int = this.limit match {
    case l if l <= 0       => DefaultFeedLimit
    case l if l > MaxLimit => MaxLimit
    case _                 => this.limit
  }
}
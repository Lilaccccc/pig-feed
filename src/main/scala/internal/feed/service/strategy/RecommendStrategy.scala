package internal.feed.service.strategy

import internal.exposure.entity.{CandidateInput, CandidateResult}
import internal.exposure.service.RecommendationService
import internal.feed.entity.{FeedPageItem, FeedRequest, FeedResult}
import internal.feed.enums.Scene
import internal.feed.enums.Scene.Recommend
import internal.infra.errors.*
import utils.base.ColoredLogger

// 使用推荐服务读取排序后的候选，再复用 Feed 卡片组装。
object RecommendStrategy extends Strategy, ColoredLogger {

  // 读取推荐候选，并按推荐服务给出的顺序组装 Feed 卡片。
  override def list(request: FeedRequest): Either[Exception, FeedResult] = {
    if request.viewerId.exists(_ <= 0L) then return Left(ErrViewerRequired())
    val limit    = request.normalizeLimit
    val viewerId = request.viewerId
    val input    = CandidateInput(
      viewerId.getOrElse(0L),
      scene.value,
      request.clientContext.getOrElse("request_id", ""),
      request.cursor,
      limit
    )
    val doing = (c: CandidateResult) => {
      val items  = c.candidates.map(item => FeedPageItem(item.videoId, Option(item.authorId), Option(item.publishedAt), Option(item.hotScore)))
      val result = FeedResult(scene, assembleFeedItems(viewerId, items), c.nextCursor, c.hasMore)
      Right(result)
    }
    RecommendationService.recommend(input).fold(err => Left(err), doing)
  }

  override def scene: Scene = Recommend
}

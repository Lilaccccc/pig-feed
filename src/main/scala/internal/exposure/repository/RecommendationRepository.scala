package internal.exposure.repository

import internal.exposure.entity.*
import internal.exposure.enums
import internal.exposure.enums.*
import internal.exposure.sql.listCandidatePoolSql
import io.circe.parser.decode
import sqala.jdbc.Cursor
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime

object RecommendationRepository extends ColoredLogger {
  def listCandidatePool(userId: Long, limit: Int): List[Candidate] = {
    if limit <= 0 then return List.empty
    db.fetchTo[Candidate](listCandidatePoolSql(userId, limit))
  }

  // 用户兴趣向量生成器，None 表示没有足够的用户数据用于生成向量
  def loadUserInterestVector(userId: Long): Option[List[Double]] = {
    import sqala.static.dsl.*
    val now = LocalDateTime.now
    val dsl = from(ViewEventModel, EmbeddingModel)
      .filter((v, e) => v.videoId == e.videoId && e.model.==(HashNgramModel))
      .filter((v, _) => v.userId == userId && v.createdAt >= now.minusSeconds(PositiveEventWindow))
      .filter((v, _) => v.eventType.in(List(EventType.Play.value, EventType.Complete.value)))
      .select((v, e) => (embeddingJson = e.embeddingJson, eventType = v.eventType, watchMs = v.watchMs, completed = v.completed))
      .sortBy((v, _) => v.createdAt.desc)
      .take(200)

    type CursorResult = (embeddingJson: String, eventType: String, watchMs: Int, completed: Boolean)
    var sum         = List.empty[Double]
    var totalWeight = 0.toDouble
    db.cursorFetch(query(dsl), 10)((c: Cursor[CursorResult]) =>
      c.data.foreach { item =>
        val eventType = EventType.value(item.eventType)
        val weight    = eventWeight(eventType, item.watchMs, item.completed)
        totalWeight += weight
        decode[List[Double]](item.embeddingJson) match {
          case Right(vector) => if vector.nonEmpty then sum = sum.zipAll(vector, 0.toDouble, 0.toDouble).map((s, v) => s + v * weight)
          case Left(err)     => error(s"解析失败: $err")
        }
      }
    )

    if sum.isEmpty || totalWeight == 0.toDouble then return None
    sum = sum.map(_ / totalWeight)
    Some(sum)
  }

  private def eventWeight(eventType: EventType, watchMs: Int, completed: Boolean): Double = eventType match {
    case enums.EventType.Exposed => 1.toDouble
    case enums.EventType.Play    =>
      val weight = 1 + watchMs.toDouble / 30000
      if weight > 2 then 2 else if completed then weight + 1 else weight
    case enums.EventType.Complete => 3.toDouble
    case enums.EventType.Skip     => 1.toDouble
    case enums.EventType.Unknown  => 0.toDouble
  }

  def loadVideoVectors(videoIds: List[Long]): Map[Long, List[Double]] = {
    if videoIds.isEmpty then return Map.empty

    type VideoVectorModel = (videoId: Long, embeddingJson: String)

    import sqala.static.dsl.*
    val dsl = from(EmbeddingModel)
      .filter(e => e.videoId.in(videoIds) && e.model == HashNgramModel)
      .select(e => (videoId = e.videoId, embeddingJson = e.embeddingJson))
    val models = db.fetchTo[VideoVectorModel](query(dsl))

    val vector = (embeddingJson: String) =>
      decode[List[Double]](embeddingJson) match {
        case Right(vector) => vector
        case Left(err)     =>
          error(s"解析失败: $err")
          List.empty
      }

    models.map(v => (v.videoId, vector(v.embeddingJson))).toMap
  }
}

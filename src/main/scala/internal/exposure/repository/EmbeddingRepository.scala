package internal.exposure.repository

import internal.exposure.entity.VideoEmbedding
import sqala.metadata.*
import utils.base.ColoredLogger
import utils.db.db

import java.time.LocalDateTime

@table("video_embedding")
final case class EmbeddingModel(
  videoId: Long,
  model: String,
  dimension: Long,
  embeddingJson: String,
  textHash: String,
  createdAt: Option[LocalDateTime],
  updatedAt: Option[LocalDateTime]
) {
  def toEntity: VideoEmbedding = VideoEmbedding(
    videoId,
    model,
    dimension,
    embeddingJson,
    textHash,
    createdAt,
    updatedAt
  )
}

object EmbeddingRepository extends ColoredLogger {
  // 使用 video_id + model upsert，重复发布事件会覆盖同模型向量。
  def saveVideoEmbedding(entity: VideoEmbedding): Option[VideoEmbedding] = {
    val now = LocalDateTime.now
    import sqala.static.dsl.*
    val dsl = from(EmbeddingModel).filter(e => e.videoId == entity.videoId && e.model.equals(entity.model)).limit(1)
    db.fetch(query(dsl)).headOption match
      case Some(value) =>
        val updateDsl = update[EmbeddingModel]
          .set(_.dimension := entity.dimension)
          .set(_.embeddingJson := entity.embeddingJson)
          .set(_.textHash := entity.textHash)
          .set(_.updatedAt := now)
          .where(e => e.videoId == entity.videoId && e.model.equals(entity.model))
        if db.execute(updateDsl) > 0 then Some(entity.copy(createdAt = value.createdAt, updatedAt = Some(now))) else None
      case None =>
        val model = entity.toModel.copy(createdAt = Some(now), updatedAt = Some(now))
        if db.insert(model) > 0 then Some(entity.copy(createdAt = Some(now), updatedAt = Some(now))) else None
  }

  // 按 video_id + model 查询视频向量。
  def findVideoEmbedding(videoId: Long, model: String): Option[VideoEmbedding] = {
    import sqala.static.dsl.*
    val dsl = from(EmbeddingModel).filter(e => e.videoId == videoId && e.model.equals(model)).limit(1)
    db.fetch(query(dsl)).headOption.map(_.toEntity)
  }
}

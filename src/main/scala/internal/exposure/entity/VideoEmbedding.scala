package internal.exposure.entity

import internal.exposure.repository.EmbeddingModel

import java.time.LocalDateTime

final case class VideoEmbedding(
  videoId: Long,
  model: String,
  dimension: Long,
  embeddingJson: String,
  textHash: String,
  createdAt: Option[LocalDateTime] = None,
  updatedAt: Option[LocalDateTime] = None
) {
  def toModel: EmbeddingModel = EmbeddingModel(
    videoId,
    model,
    dimension,
    embeddingJson,
    textHash,
    createdAt,
    updatedAt
  )
}

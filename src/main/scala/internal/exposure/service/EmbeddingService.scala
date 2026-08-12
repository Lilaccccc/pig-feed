package internal.exposure.service

import com.google.common.hash.Hashing
import internal.exposure.entity.*
import internal.exposure.enums.*
import internal.exposure.repository.EmbeddingRepository
import internal.exposure.utils.Hash.fnv1a64
import internal.infra.errors.*
import internal.video.entity.VideoPublishedEvent
import utils.base.{ColoredLogger, json}

import java.nio.charset.StandardCharsets
import scala.collection.mutable.ListBuffer

object EmbeddingService extends ColoredLogger {
  // 根据视频发布事件生成并保存视频内容向量
  def generateForPublishedVideo(event: VideoPublishedEvent): Either[Exception, Option[GenerateVideoEmbeddingResult]] = {
    val eventId = event.videoId
    if eventId <= 0L then return Right(None)
    val text    = buildVideoText(event.title, event.description)
    val vector  = vectorize(text)
    val context = vector.json
    EmbeddingRepository
      .saveVideoEmbedding(VideoEmbedding(eventId, HashNgramModel, vector.size, context, textHash(text)))
      .map(embedding => Right(Some(GenerateVideoEmbeddingResult(embedding, true))))
      .getOrElse(Left(ErrSaveVideoEmbeddingFailed()))
  }

  // 把视频标题和简介拼成稳定向量输入。
  private def buildVideoText(title: String, description: String): String = {
    if description.isBlank then return title
    if title.isBlank then return description
    s"$title\n$description"
  }

  // 使用字符 n-gram 和 token 特征生成稳定向量，并做 L2 归一化。
  private def vectorize(text: String): List[Double] = {
    val normalized = normalizeText(text)
    if normalized.isBlank then return List.empty
    val vector = ListBuffer.fill(HashNgramDimension)(0.0)
    normalized.split("\\s+").foreach(token => addFeature(vector, s"tok:$token", 1.0))
    // 从文本中提取长度为 2 和 3 的所有连续字符组合（2-gram 3-gram）作为特征添加到向量中
    val chars = normalized.replace(" ", "")
    (2 to 3).foreach(n => if chars.length >= n then chars.sliding(n).foreach(gram => addFeature(vector, gram, 1.0)))
    normalizeVector(vector)
    vector.toList
  }

  // 简单分词
  private def normalizeText(text: String): String = {
    val t = text.trim.toLowerCase
    if t.isBlank then return ""
    val builder       = StringBuilder()
    var previousSpace = true
    text.foreach(ch =>
      val cond = () =>
        (Character.isWhitespace(ch) || Character.isISOControl(ch)
          || (Character.isLowerCase(ch) && Character.isISOControl(ch)))
          && !previousSpace

      if Character.isLetterOrDigit(ch) then {
        builder.append(ch)
        previousSpace = false
      } else if cond() then {
        builder.append(' ')
        previousSpace = true
      }
    )
    builder.toString.trim
  }

  // 把文本特征名映射到固定长度向量的某个位置上，加上或减去一个权重值
  private def addFeature(vector: ListBuffer[Double], feature: String, weight: Double): Unit = {
    if vector.isEmpty then return
    val hash  = fnv1a64(feature)
    val index = (Math.abs(hash) % vector.length).toInt
    val sign  = if hash < 0 then -1.0 else 1.0
    vector(index) += sign * weight
  }

  private def normalizeVector(vector: ListBuffer[Double]): Unit = {
    val sum = vector.foldLeft(0.0)((acc, value) => acc + value * value)
    if sum == 0.0 then return
    val norm = Math.sqrt(sum)
    vector.map(_ / norm)
  }

  // 计算文本哈希，方便重复发布事件判断内容是否变化
  private def textHash(text: String): String = Hashing.sha256.hashString(text.trim, StandardCharsets.UTF_8).toString.trim
}

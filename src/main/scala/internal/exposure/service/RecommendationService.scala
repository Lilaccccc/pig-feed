package internal.exposure.service

import internal.exposure.entity.*
import internal.exposure.enums.*
import internal.exposure.repository.RecommendationRepository
import internal.feed.utils.decodeCursor
import internal.infra.enums.*
import internal.infra.errors.*
import utils.base.ColoredLogger

import java.time.{Duration, LocalDateTime}
import scala.collection.mutable.ListBuffer

object RecommendationService extends ColoredLogger {
  def recommend(input: CandidateInput): Either[Exception, CandidateResult] = {
    val userId    = input.userId
    val limit     = input.limit.normalizeLimit
    val scene     = input.scene.trim
    val requestId = input.requestId.trim

    if userId <= 0L then return Left(ErrInvalidUserID())
    if limit <= 0L || limit > MaxLimit then return Left(ErrInvalidLimit())
    if scene.isBlank then return Left(ErrEmptyScene())
    if scene.length > MaxSceneLength then return Left(ErrSceneTooLong())
    if requestId.length > MaxRequestIDLength then return Left(ErrRequestIDTooLong())

    parseRecommendationCursor(input.cursor)
      .flatMap(_ => {
        val poolLimit = candidatePoolLimit(limit)
        val pool      = RecommendationRepository.listCandidatePool(userId, poolLimit)
        rankCandidates(userId, pool)
      })
      .fold(
        err => Left(err),
        candidates => {
          val hasMore    = candidates.nonEmpty
          val nextCursor = if hasMore then {
            val last = candidates.last
            RecommendationCursor(last.rankScore, last.publishedAt, last.videoId).encodeBase64
          } else ""
          val ranked  = interleaveByAuthor(candidates)
          val request = CandidateResult(userId, scene, requestId, ranked, nextCursor, hasMore)
          Right(request)
        }
      )
  }

  private def parseRecommendationCursor(raw: String): Either[Exception, Option[RecommendationCursor]] = decodeCursor(raw)
    .flatMap(bytes => RecommendationCursor.decodeBytes(bytes))
    .flatMap(cursor => Either.cond(cursor.videoId > 0L && java.lang.Double.isFinite(cursor.rankScore), cursor, ErrInvalidCursor()))
    .fold(err => if err.isInstanceOf[ErrCursorIsBlank] then Right(None) else Left(err), result => Right(Some(result)))

  private def candidatePoolLimit(limit: Int): Int = {
    val poolLimit = limit * CandidatePoolMultiplier
    if poolLimit < MinCandidatePoolSize then MinCandidatePoolSize
    else if poolLimit > MaxCandidatePoolSize then MaxCandidatePoolSize
    else poolLimit
  }

  private def rankCandidates(userId: Long, pool: List[Candidate]): Either[Exception, List[Candidate]] = {
    if pool.isEmpty then return Right(List.empty)
    val videoIds      = pool.map(_.videoId)
    val vectors       = RecommendationRepository.loadVideoVectors(videoIds)
    val userVectors   = RecommendationRepository.loadUserInterestVector(userId)
    val hasUserVector = userVectors.nonEmpty
    val now           = LocalDateTime.now
    val toMap         = (candidate: Candidate) => {
      val similarity = if hasUserVector then {
        val vector = vectors(candidate.videoId)
        userVectors
          .filter(_ => vector.nonEmpty)
          .map(userVector => cosineSimilarity(userVector, vector))
          .getOrElse(0.toDouble)
      } else 0.toDouble
      val freshness = freshnessScore(now, candidate.publishedAt)
      candidate.copy(
        freshnessScore = freshness,
        rankScore = rankScore(similarity, candidate.hotScore, freshness, hasUserVector),
        reason = recommendationReason(hasUserVector, similarity, candidate.hotScore)
      )
    }
    val sort = (l: Candidate, r: Candidate) =>
      if l.rankScore != r.rankScore then l.rankScore > r.rankScore
      else if !l.publishedAt.equals(r.publishedAt) then l.publishedAt.isAfter(r.publishedAt)
      else l.videoId > r.videoId
    Right(pool.map(toMap).sortWith(sort))
  }

  // 计算两个已归一化或未归一化向量的余弦相似度
  private def cosineSimilarity(left: List[Double], right: List[Double]): Double = {
    if left.size != right.size then
      return {
        error(ErrDimensionMismatch().getMessage)
        0.toDouble
      }
    val (dot, leftNorm, rightNorm) = left.zip(right).foldLeft((0.0, 0.0, 0.0)) { case ((d, ln, rn), (l, r)) =>
      (d + l * r, ln + l * l, rn + r * r)
    }
    if leftNorm == 0 || rightNorm == 0 then return 0.toDouble
    dot / (math.sqrt(leftNorm) * math.sqrt(rightNorm))
  }

  private def freshnessScore(now: LocalDateTime, publishedAt: LocalDateTime): Double = {
    val hours = Duration.between(publishedAt, now).toHours
    if hours < 0 then 1 else 1 / (1 + hours / 72)
  }

  private def rankScore(similarity: Double, hotScore: Int, freshness: Double, hasUserVector: Boolean): Double = {
    val hot = math.log1p(math.max(hotScore, 0).toDouble) / 10
    if hasUserVector then similarity * 0.70 + hot * 0.20 + freshness * 0.10
    else hot * 0.65 + freshness * 0.35
  }

  private def recommendationReason(hasUserVector: Boolean, similarity: Double, hotScore: Int): String = {
    if hasUserVector && similarity > 0.05 then return "interest_match"
    if hotScore > 0 then return "hot"
    "fresh"
  }

  private def interleaveByAuthor(candidates: List[Candidate]): List[Candidate] = {
    if candidates.size <= 3 then return candidates
    val (output, delayed) = candidates.foldLeft((ListBuffer.empty[Candidate], ListBuffer.empty[Candidate])) { case ((output, delayed), item) =>
      (output, delayed) match
        case (Nil, _)                                   => (output += item, delayed)
        case _ if output.last.authorId == item.authorId => (output, delayed += item)
        case _                                          => (output += item, delayed)
    }

    @annotation.tailrec
    def process(output: ListBuffer[Candidate], delayed: ListBuffer[Candidate]): List[Candidate] = {
      val (before, after) = delayed.span(item => output.nonEmpty && output.last.authorId == item.authorId)
      if after.isEmpty then {
        output ++= delayed
        output.toList
      } else {
        output += after.head
        val remaining = before ++ after.tail
        process(output, remaining)
      }
    }

    process(output, delayed)
  }
}

package internal.upload.utils

import internal.infra.errors.*
import io.circe.Codec
import utils.base.config.enums.UploadConfig
import utils.base.decode

import java.io.{BufferedReader, InputStreamReader}
import java.nio.file.{Path, Paths}
import scala.util.Using

object FFprobeExecutor {
  def probeVideo(path: String): Either[Exception, Option[ProbeResult]] = {
    val pb = ProcessBuilder(
      UploadConfig.ffprobe,
      "-v",
      "error",
      "-show_entries",
      "stream=codec_type,codec_name,width,height:format=duration",
      "-of",
      "json",
      path
    )

    // 合并错误流到标准输出（便于统一读取）
    pb.redirectErrorStream(true)

    val process = pb.start
    val output  = StringBuilder()

    try {
      Using(BufferedReader(InputStreamReader(process.getInputStream))) { reader =>
        Iterator
          .continually(reader.readLine)
          .takeWhile(_ != null)
          .foreach(output.append)
      }

      // 等待进程结束
      val exitCode = process.waitFor
      val entity   = () => output.toString.decode[ProbeResult].toOption
      Either.cond(exitCode == 0, entity(), ErrVideoToolUnavailable("ffprobe failed"))
    } catch {
      case e: Exception => Left(e)
    } finally process.destroy()
  }

  def faststartVideo(path: String): Either[Exception, Path] = {
    val target = s"$path.faststart.mp4"
    val pb     = ProcessBuilder(
      UploadConfig.ffmpeg,
      "-y",
      "-i",
      path,
      "-map",
      "0",
      "-c",
      "copy",
      "-movflags",
      "+faststart",
      "-f",
      "mp4",
      target
    )

    // 合并错误流到标准输出（便于统一读取）
    pb.redirectErrorStream(true)

    val process = pb.start
    val output  = StringBuilder()

    try {
      Using(BufferedReader(InputStreamReader(process.getInputStream))) { reader =>
        Iterator
          .continually(reader.readLine)
          .takeWhile(_ != null)
          .foreach(output.append)
      }

      // 等待进程结束
      val exitCode = process.waitFor
      Either.cond(exitCode == 0, Paths.get(target), ErrVideoToolUnavailable("ffmpeg failed"))
    } catch {
      case e: Exception => Left(e)
    } finally process.destroy()
  }

  final case class ProbeResult(
    streams: List[ProbeStream],
    format: ProbeFormat
  ) derives Codec

  final case class ProbeStream(
    codec_type: String,
    codec_name: String,
    width: Option[Int],
    height: Option[Int]
  ) derives Codec

  final case class ProbeFormat(duration: String) derives Codec {
    def durationVal: Double = duration.trim.toDoubleOption.getOrElse(0.0)
  }
}

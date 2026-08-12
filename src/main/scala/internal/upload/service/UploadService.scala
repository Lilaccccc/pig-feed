package internal.upload.service

import internal.upload.controller.dto.UploadResponse
import internal.upload.enums.FileEnums.*
import internal.upload.enums.Kind
import internal.upload.enums.Kind.*
import internal.infra.errors.*
import internal.upload.utils.*
import internal.upload.utils.FFprobeExecutor.*
import sttp.model.Part
import utils.base.ColoredLogger
import utils.base.config.enums.UploadConfig

import java.io.File
import java.nio.file.{Path, Paths}

object UploadService extends ColoredLogger {
  // 接收 multipart/form-data 文件，并按 kind 保存到不同子目录。
  def create(part: Part[File]): Either[Exception, UploadResponse] = {
    val response = (fileSize: Long, kindRaw: String, filename: String) =>
      UploadResponse(
        url = s"/${UploadConfig.root}/$kindRaw/$filename",
        kind = kindRaw,
        filename = filename,
        size = fileSize
      )

    Kind
      .normalizeKind(part.headers)
      .map((kind: Kind, mime: String) => {
        debug(s"kind = $kind mime = $mime")
        val file     = part.body
        val tempFile = file.getAbsolutePath
        val target   = randomNameWithMime(mime)
        kind
          .validateUploadSize(file)
          .flatMap(_ => FFprobeExecutor.probeVideo(tempFile))
          .flatMap(opt => validateVideoMetadata(kind, opt))
          .flatMap(_ => faststart(mime, tempFile))
          .flatMap(fastPath => kind.upload(fastPath, target))
          .fold(
            err => Left(err.asInstanceOf[Exception]),
            _ => Right(response(file.length, kind.raw, target))
          )
      })
      .getOrElse(Left(ErrInvalidUploadExtension()))
  }

  private def validateVideoMetadata(kind: Kind, metadataOpt: Option[ProbeResult]): Either[Exception, Unit] = {
    if metadataOpt.isEmpty && kind == Kind.Image then return Right(())
    else if metadataOpt.isEmpty then return Left(ErrInvalidUploadKind())

    val metadata = metadataOpt.get

    val duration = metadata.format.durationVal
    if duration <= 0.0 then return Left(ErrInvalidVideoMetadata())
    if duration > MaxVideoDurationSeconds.value then return Left(ErrVideoTooLong())

    val validateVideo = (stream: ProbeStream) => {
      val width     = stream.width.getOrElse(0)
      val height    = stream.height.getOrElse(0)
      val codecName = stream.codec_name.trim.toLowerCase
      debug(s"codecName::$codecName")
      Either
        .cond(width > 0 || height > 0, (width, height), ErrInvalidVideoMetadata())
        .flatMap((w, h) => Either.cond(w <= MaxVideoDimension.value || h <= MaxVideoDimension.value, codecName, ErrUnsupportedVideoCodec()))
//        .flatMap(n => Either.cond(videoCodecNameMap.contains(n), 1, ErrUnsupportedVideoCodec()))
        .flatMap(n => Either.cond(true, 1, ErrUnsupportedVideoCodec()))
    }

    val validateAudio = (stream: ProbeStream) => {
      val codecName = stream.codec_name.trim.toLowerCase
//      Either.cond(audioCodecTypeMap.contains(codecName), (), ErrUnsupportedVideoCodec())
      Either.cond(true, (), ErrUnsupportedVideoCodec())
    }

    metadata.streams
      .foldLeft[Either[Exception, Int]](Right(0)) { (acc, stream) =>
        acc match {
          // 如果已经有错误，直接返回，不再继续，短路。
          case Left(err)    => Left(err)
          case Right(count) =>
            stream.codec_type.trim.toLowerCase match {
              case "video" => validateVideo(stream).fold(err => Left(err), int => Right(count + int))
              case "audio" => validateAudio(stream).fold(err => Left(err), _ => Right(count))
              case _       => Right(count)
            }
        }
      }
      .flatMap(int => Either.cond(int > 0, (), ErrInvalidVideoMetadata()))
  }

  private def videoCodecNameMap: Map[String, Unit] = Map(
    "h264" -> (),
    "h265" -> (),
    "hevc" -> (),
    "vp8"  -> (),
    "vp9"  -> (),
    "av1"  -> ()
  )

  private def audioCodecTypeMap: Map[String, Unit] = Map(
    "aac"    -> (),
    "mp3"    -> (),
    "opus"   -> (),
    "vorbis" -> ()
  )

  private def faststart(mime: String, video: String): Either[Exception, Path] = {
    if !(mime.equals("mp4") || mime.equals("mov")) then return Right(Paths.get(video))
    FFprobeExecutor.faststartVideo(video)
  }
}

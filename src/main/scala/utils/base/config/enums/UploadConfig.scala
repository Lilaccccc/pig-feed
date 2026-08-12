package utils.base.config.enums

import utils.base.config
import utils.base.config.Config

enum UploadConfig(val field: String) extends Config("upload") {
  private case Root extends UploadConfig("root")
  private case FFmpeg extends UploadConfig("ffmpeg")
}

object UploadConfig {
  def root = config.get[String](Root)
  def ffmpeg = s"${config.get[String](FFmpeg)}ffmpeg.exe"
  def ffprobe = s"${config.get[String](FFmpeg)}ffprobe.exe"
  def ffplay = s"${config.get[String](FFmpeg)}ffplay.exe"
}

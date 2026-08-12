package internal.upload.controller.dto

import io.circe.Codec
import sttp.tapir.Schema

// 上传成功后返回给前端的文件访问地址和元信息。
final case class UploadResponse(
  url: String,
  kind: String,
  filename: String,
  size: Long
) derives Codec, Schema

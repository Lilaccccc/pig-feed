package internal.upload.controller

import internal.infra.route.RouteOps.*
import internal.upload.controller.dto.*
import internal.upload.controller.handler.UploadHandler
import internal.upload.enums.FileEnums
import internal.upload.service.UploadService
import sttp.tapir.server.model.EndpointExtensions.*
import utils.result.R
import utils.route.Controller

class UploadController extends Controller {
  override val serverEndpointList = List(uploads)

  private def uploads = this.endpoint.post
    .in("uploads" and multipartBody[UploadForm])
    // 限制的是整个请求体的总大小，而不是单个文件大小，避免大文件撑爆内存或磁盘。
    .maxRequestBodyLength(maxBytes = FileEnums.MaxUploadBytes.value)
    .out(jsonBody[R[UploadResponse]])
    .security((_, part) => UploadHandler.create(part.file))
}

package internal.upload.controller.handler

import internal.upload.controller.dto.UploadResponse
import internal.upload.service.UploadService
import sttp.model.Part
import utils.base.ColoredLogger
import utils.result.throws

import java.io.File

object UploadHandler extends ColoredLogger {
  def create(part: Part[File]): UploadResponse = try {
    UploadService.create(part).fold(err => err.throws(using this), response => response)
  } catch {
    case e: Exception => error(e.getMessage); throw e
  }
}

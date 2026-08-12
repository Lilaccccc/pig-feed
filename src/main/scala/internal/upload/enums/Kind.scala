package internal.upload.enums

import internal.infra.errors.*
import internal.upload.enums.FileEnums.*
import sttp.model.Header
import utils.result.ErrorResult

import scala.collection.immutable.Seq

enum Kind(val raw: String) {
  case Video extends Kind("video")
  case Image extends Kind("image")
}

object Kind {
  def normalizeKind(headers: Seq[Header]): Option[(Kind,String)] = {
    val fromContentType = (v: String) =>
      v match {
        case t if t.startsWith(Video.raw) => (Video, t.split("/")(1))
        case t if t.startsWith(Image.raw) => (Image, t.split("/")(1))
        case _ => None.asInstanceOf[(Kind, String)]
      }

    headers
      .find(_.is("Content-Type"))
      .map(_.value)
      .map(fromContentType)
  }

  extension (kind: Kind) {
    def validateUploadSize(file: java.io.File): Either[ErrorResult, Unit] = {
      val size = file.length
      if size <= 0 then return Left(ErrInvalidUploadMIME())
      if kind == Kind.Video && size > MaxVideoBytes.value then return Left(ErrUploadTooLarge())
      if kind == Kind.Image && size > MaxImageBytes.value then return Left(ErrUploadTooLarge())
      if size > MaxUploadBytes.value then return Left(ErrUploadTooLarge())
      Right(())
    }
  }
}

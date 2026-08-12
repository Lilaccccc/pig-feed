package internal.feed.utils

import internal.infra.errors.*

import java.util.Base64
import scala.util.Try

def decodeCursor(raw: String): Either[Exception, Array[Byte]] = {
  val trim = raw.trim
  if trim.isBlank then return Left(ErrCursorIsBlank())
  val example = () => Try(Base64.getDecoder.decode(trim)).fold(err => Left(ErrInvalidCursor()), result => Right(result))
  Try(Base64.getUrlDecoder.decode(trim)).fold(err => example(), result => Right(result))
}

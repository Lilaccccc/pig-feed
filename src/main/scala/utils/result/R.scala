package utils.result

import io.circe.{Decoder, Encoder, HCursor, Json}
import utils.base.ColoredLogger
import sttp.tapir.*

final case class R[T](
  code: Int,
  msg: String,
  data: Option[T] = None
)

object R extends ColoredLogger {
  // 序列化
  given [T](using e: Encoder[T]): Encoder[R[T]] =
    (a: R[T]) =>
      Json.obj(
        ("code", Json.fromInt(a.code)),
        ("msg", Json.fromString(a.msg)),
        (
          "data",
          a.data match {
            case Some(value) => e(value)
            case None        => Json.Null
          }
        )
      )

  // 反序列化
  given [T](using d: Decoder[T]): Decoder[R[T]] =
    (c: HCursor) =>
      for {
        code <- c.downField("code").as[Int]
        msg  <- c.downField("msg").as[String]
        data <- c.downField("data").as[Option[T]]
      } yield R(code, msg, data)

  inline given [T: Schema]: Schema[R[T]] = Schema.derived[R[T]]

  def apply[T](data: T): R[T]                                     = R(StatusCode.Success.code, StatusCode.Success.message, Some(data))
  def apply[T](statusCode: StatusCode, data: T): R[T]             = R(statusCode.code, statusCode.message, Some(data))
  def status(statusCode: StatusCode): R[StatusCode]               = R(statusCode.code, statusCode.message, None)
  def error[T](errMsg: String = StatusCode.Fail.message): R[T]    = R(StatusCode.Fail.code, errMsg, None)

  def errorWithCode[T](errCode: Int = StatusCode.Fail.code, errMsg: String = StatusCode.Fail.message): R[T] = R(errCode, errMsg, None)
}

package utils.result

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

enum StatusCode(val code: Int, val message: String) {
  case Success      extends StatusCode(1000, "请求成功")
  case Fail         extends StatusCode(1001, "请求失败")
  case ParamError   extends StatusCode(1002, "参数错误")
  case Unauthorized extends StatusCode(1003, "未授权")
  case NotFound     extends StatusCode(1004, "资源不存在")
  case Forbidden    extends StatusCode(1005, "禁止访问")
  case DataNotFound extends StatusCode(1006, "未查询到数据")
} 

object StatusCode {
  given Encoder[StatusCode] = deriveEncoder[StatusCode]
  given Decoder[StatusCode] = deriveDecoder[StatusCode]
  given Schema[StatusCode] = Schema.derived[StatusCode]
}

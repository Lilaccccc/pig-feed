package utils.result

import utils.base.ColoredLogger

trait ErrorResult(val code: Int)

extension (e: Exception) {
  def throws(using logger: ColoredLogger) = {
    logger.error(e.getMessage)
    throw e
  }

  def throws(code: Int, msg: String)(using logger: ColoredLogger) = {
    logger.error(s"${(e.getMessage, msg)}")
    throw new Exception(msg) with ErrorResult(code)
  }

  def throws[T <: Exception & ErrorResult](err: T)(using logger: ColoredLogger) = {
    logger.error(s"${(e.getMessage, err.getMessage)}")
    throw err
  }
}

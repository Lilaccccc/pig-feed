package utils.base

import com.typesafe.scalalogging.LazyLogging
import kyo.Ansi.*
import sourcecode.*
import sqala.jdbc.Logger

trait ColoredLogger extends LazyLogging {
  def error(message: String)(using file: File, line: Line): Unit = logger.error(s"[${file.value}:${line.value}] ${message.red}")
  def warn(message: String)(using file: File, line: Line): Unit  = logger.warn(s"[${file.value}:${line.value}] ${message.yellow}")
  def info(message: String)(using file: File, line: Line): Unit  = logger.info(s"[${file.value}:${line.value}] ${message.green}")
  def debug(message: String)(using file: File, line: Line): Unit = logger.debug(s"[${file.value}:${line.value}] ${message.blue}")

  given (using file: File, line: Line): Logger = Logger(info => debug(info)(using file, line))
}

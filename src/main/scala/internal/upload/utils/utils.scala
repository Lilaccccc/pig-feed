package internal.upload.utils

import internal.upload.enums.Kind
import internal.infra.errors.*
import utils.base.ColoredLogger
import utils.base.config.enums.UploadConfig
import utils.base.idgenerator.IdGenerator
import utils.result.throws

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import scala.util.Try

def randomNameWithMime(ext: String) = {
  val now       = System.currentTimeMillis
  val randomHex = IdGenerator.hex16Id(36)
  s"$now-$randomHex.$ext"
}

def prepareUploadDirectory(using logger: ColoredLogger) = {
  val init = () => Kind.values.foreach(k => Files.createDirectories(k.targetDir))
  Try(init()).fold(err => err.asInstanceOf[Exception].throws(using logger), _ => logger.info("初始化文件上传目录成功"))
}

extension (k: Kind) {
  def targetDir                    = Paths.get(UploadConfig.root, k.raw)
  def targetPath(filename: String) = Paths.get(UploadConfig.root, k.raw, filename)

  def upload(sourcePath: Path, targetFilename: String): Either[ErrSaveFile, Path] = {
    val targetPath = k.targetPath(targetFilename)
    val save = () => {
      val bytes  = Files.readAllBytes(sourcePath)
      val stream = ByteArrayInputStream(bytes)
      Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING)
    }
    Try(save()).fold(err => Left(ErrSaveFile()), _ => Right(targetPath))
  }
}

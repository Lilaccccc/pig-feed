package internal.upload.controller.dto

import sttp.model.Part
import sttp.tapir.Schema

import java.io.File

/**
 * Part[File]：Tapir 会将上传的文件保存为临时 File 对象，请求处理完后自动清理。
 * Part[Array[Byte]]：直接将文件内容读取到内存中（适合小文件）。
 * Part 类型包含文件名、Content-Type 等元数据。
 */
case class UploadForm(file: Part[File]) derives Schema
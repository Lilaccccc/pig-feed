package internal.upload.enums

enum FileEnums(val value: Int) {
  case MaxUploadBytes          extends FileEnums(1024 << 20)
  case MaxVideoBytes           extends FileEnums(1024 << 20)
  case MaxImageBytes           extends FileEnums(20 << 20)
  case MaxVideoDurationSeconds extends FileEnums(10 * 240)
  case MaxVideoDimension       extends FileEnums(3840)
  case SniffBytes              extends FileEnums(1024)
}

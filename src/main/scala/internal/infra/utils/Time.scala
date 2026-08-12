package internal.infra.utils

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object Time {
  def rfc3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'").withZone(ZoneOffset.UTC)
  def rfc3339WithNotZ = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS").withZone(ZoneOffset.UTC)
}

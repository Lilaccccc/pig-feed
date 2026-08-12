package internal.exposure.utils

import java.nio.charset.StandardCharsets

object Hash {
  def fnv1a64(text: String): Long = {
    var hash  = 0xcbf29ce484222325L
    val prime = 0x100000001b3L
    val bytes = text.getBytes(StandardCharsets.UTF_8)
    bytes.foreach(b => {
      hash ^= (b & 0xff)
      hash *= prime
    })
    hash
  }
}

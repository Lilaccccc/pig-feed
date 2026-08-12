package utils.base.idgenerator

import java.math.BigInteger
import java.security.SecureRandom

object IdGenerator {
  private lazy val snowGenerator = SnowflakeIdGenerator(0, 0)
  
  def snowId: Long = snowGenerator.generateUniqueId

  def hex16Id(length: Int): String = {
    val bytes = new Array[Byte](length / 2)
    SecureRandom().nextBytes(bytes)
    BigInteger(1, bytes).toString(16)
  }

  def main(args: Array[String]): Unit = println(hex16Id(22))
}


package utils.base.config.enums

import utils.base.config
import utils.base.config.Config

enum JwtConfig(val field: String) extends Config("jwt") {
  private case Secret extends JwtConfig("secret")
  private case AccessTtl extends JwtConfig("accessTtl")
}

object JwtConfig {
  def secret = config.get[String](Secret).trim
  def accessTtl = config.get[Int](AccessTtl)
}
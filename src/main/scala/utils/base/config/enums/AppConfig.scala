package utils.base.config.enums

import utils.base.config
import utils.base.config.Config

enum AppConfig(val field: String) extends Config("server") {
  private case Name extends AppConfig("name")
  private case Port extends AppConfig("port")
}

object AppConfig {
  def name = config.get[String](Name)
  def port = config.get[Int](Port)
}
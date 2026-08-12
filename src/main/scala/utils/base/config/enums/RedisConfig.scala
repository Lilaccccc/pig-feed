package utils.base.config.enums

import utils.base.config
import utils.base.config.Config

enum RedisConfig(val field: String) extends Config("redis") {
  private case Host                        extends RedisConfig("host")
  private case Port                        extends RedisConfig("port")
  private case Password                    extends RedisConfig("password")
  private case KeyPrefix                   extends RedisConfig("keyPrefix")
  private case Database                    extends RedisConfig("database")
  private case MaxTotal                    extends RedisConfig("maxTotal")
  private case MaxIdle                     extends RedisConfig("maxIdle")
  private case MinIdle                     extends RedisConfig("minIdle")
  private case EvictorShutdownTimeout      extends RedisConfig("evictorShutdownTimeout")
  private case TestOnBorrow                extends RedisConfig("testOnBorrow")
  private case TestOnReturn                extends RedisConfig("testOnReturn")
  private case TestWhileIdle               extends RedisConfig("testWhileIdle")
  private case TimeoutMillis               extends RedisConfig("timeoutMillis")
  private case ConnectionTimeoutMillis     extends RedisConfig("connectionTimeoutMillis")
  private case SocketTimeoutMillis         extends RedisConfig("socketTimeoutMillis")
  private case BlockingSocketTimeoutMillis extends RedisConfig("blockingSocketTimeoutMillis")
  private case ClearnOnStart               extends RedisConfig("clearnOnStart")
}

object RedisConfig {
  def maxTotal                    = config.get[Int](MaxTotal)
  def maxIdle                     = config.get[Int](MaxIdle)
  def minIdle                     = config.get[Int](MinIdle)
  def evictorShutdownTimeout      = config.get[Int](EvictorShutdownTimeout)
  def testOnBorrow                = config.get[Boolean](TestOnBorrow)
  def testOnReturn                = config.get[Boolean](TestOnReturn)
  def testWhileIdle               = config.get[Boolean](TestWhileIdle)
  def host                        = config.get[String](Host)
  def port                        = config.get[Int](Port)
  def password                    = config.get[String](Password)
  def database                    = config.get[Int](Database)
  def timeoutMillis               = config.get[Int](TimeoutMillis)
  def connectionTimeoutMillis     = config.get[Int](ConnectionTimeoutMillis)
  def socketTimeoutMillis         = config.get[Int](SocketTimeoutMillis)
  def blockingSocketTimeoutMillis = config.get[Int](BlockingSocketTimeoutMillis)
  def clearnOnStart               = config.get[Boolean](ClearnOnStart)
  def keyPrefix                   = config.get[String](KeyPrefix)
}

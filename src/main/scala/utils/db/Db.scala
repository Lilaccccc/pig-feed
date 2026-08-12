package utils.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import sqala.jdbc.JdbcContext
import sqala.metadata.MysqlDialect
import utils.base.config.enums.DbConfig.*

lazy val db: JdbcContext = JdbcContext(dataSource(), MysqlDialect, true)

// 初始化数据库
def migrate(url: String = url, username: String = username, password: String = password): Unit = if migrateOnStart then
  Flyway.configure().dataSource(url, username, password).baselineOnMigrate(true).baselineVersion("0").load().migrate()

private def dataSource(
  url: String = url,
  username: String = username,
  password: String = password
): HikariDataSource = {
  // 关闭 Hikari 连接池日志打印
  LoggerFactory
    .getLogger("com.zaxxer.hikari")
    .asInstanceOf[ch.qos.logback.classic.Logger]
    .setLevel(ch.qos.logback.classic.Level.ERROR)
  val hc = HikariConfig()
  hc.setJdbcUrl(url)
  hc.setUsername(username)
  hc.setPassword(password)
  hc.setMaximumPoolSize(maximumPoolSize)
  hc.setMinimumIdle(minimumIdle)
  hc.setIdleTimeout(idleTimeout)
  hc.setMaxLifetime(maxLifetime)
  hc.setThreadFactory(Thread.ofVirtual().factory())
  HikariDataSource(hc)
}

extension (s: String) {
  def sql = sqala.dynamic.native.NativeSql(s, Array())
}

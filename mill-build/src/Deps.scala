package millbuild

import mill.javalib.*

object Deps {
  val HikariCP = mvn"com.zaxxer:HikariCP:7.1.0"
  val amqpClient = mvn"com.rabbitmq:amqp-client:5.32.0"
  val chimney = mvn"io.scalaland::chimney:2.0.0-M3"
  val commonsCodec = mvn"commons-codec:commons-codec:1.22.0"
  val flywayMysql = mvn"org.flywaydb:flyway-mysql:12.9.0"
  val guava = mvn"com.google.guava:guava:33.6.0-jre"
  val jbcrypt = mvn"org.mindrot:jbcrypt:0.4"
  val jedis = mvn"redis.clients:jedis:7.5.2"
  val jwtCore = mvn"com.github.jwt-scala::jwt-core:11.0.4"
  val kyoPrelude = mvn"io.getkyo::kyo-prelude:1.0.0-RC4"
  val logbackClassic = mvn"ch.qos.logback:logback-classic:1.5.34"
  val mysqlConnectorJ = mvn"com.mysql:mysql-connector-j:8.0.33"
  val pekkoHttpCors = mvn"org.apache.pekko::pekko-http-cors:1.3.0"
  val scalaLogging = mvn"com.typesafe.scala-logging::scala-logging:3.9.6"
  val scalaParallelCollections =
    mvn"org.scala-lang.modules::scala-parallel-collections:1.2.0"
  val sqalaJdbc_3 = mvn"com.wz7982:sqala-jdbc_3:0.7.5"
  val tapirJsonCirce =
    mvn"com.softwaremill.sttp.tapir::tapir-json-circe:1.13.23"
  val tapirPekkoHttpServer =
    mvn"com.softwaremill.sttp.tapir::tapir-pekko-http-server:1.13.23"
}

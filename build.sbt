scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .settings(
    name := "feed-scala",
    idePackagePrefix := Some("org.api"),
    // 使用 sbt "show discoveredMainClasses" 命令查看主类列表
    // 打包命令：sbt clean assembly
    assembly / mainClass := Some("org.api.Application"),
    // 合并策略设置，当依赖冲突时需要配置，根据详细情况修改
    assembly / assemblyMergeStrategy := {
      // native-image 配置
      case PathList("META-INF", "native-image", xs@_*) => MergeStrategy.first
      // module-info 文件
      case PathList("META-INF", "versions", xs@_*) => MergeStrategy.first
      // 保留 SLF4J 的服务文件
      case PathList("META-INF", "services", xs@_*) => MergeStrategy.concat
      // 其他 META-INF 文件
      case PathList("META-INF", xs@_*) => MergeStrategy.discard
      // protobuf 文件
      case PathList("google", "protobuf", xs@_*) => MergeStrategy.first
      // smile 库文件
      case PathList("smile", "plot", "vega", xs@_*) => MergeStrategy.first
      // module-info 文件
      case PathList("module-info.class") => MergeStrategy.discard
      // 其他 module-info 文件
      case x if x.endsWith("/module-info.class") => MergeStrategy.discard
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )

libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0"
libraryDependencies += "io.scalaland" %% "chimney" % "2.0.0-M3"
libraryDependencies += "redis.clients" % "jedis" % "7.5.2"
libraryDependencies += "commons-codec" % "commons-codec" % "1.22.0"
libraryDependencies += "com.google.guava" % "guava" % "33.6.0-jre"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-pekko-http-server" % "1.13.23",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.13.23",
  "org.apache.pekko" %% "pekko-http-cors" % "1.3.0"
)

libraryDependencies ++= Seq(
  "io.getkyo" %% "kyo-prelude" % "1.0.0-RC4",
  "ch.qos.logback" % "logback-classic" % "1.5.34",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6"
)

libraryDependencies ++= Seq(
  "com.wz7982" % "sqala-jdbc_3" % "0.7.5",
  "com.zaxxer" % "HikariCP" % "7.1.0",
  "com.mysql" % "mysql-connector-j" % "8.0.33",
  "org.flywaydb" % "flyway-mysql" % "12.9.0"
)

libraryDependencies ++= Seq(
  "org.mindrot" % "jbcrypt" % "0.4",
  "com.github.jwt-scala" %% "jwt-core" % "11.0.4"
)

libraryDependencies += "com.rabbitmq" % "amqp-client" % "5.32.0"
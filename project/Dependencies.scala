import sbt._

object Dependencies {

  object versions {
    val play = "2.5.10"
  }

  val neo4j =
    Seq(
      "org.neo4j.driver" % "neo4j-java-driver" % "1.0.6",
      "org.neo4j" % "neo4j" % "3.0.7"
    )

  val playJson =
    Seq(
      "com.typesafe.play" %% "play-json" % versions.play
    )

  val playWS =
    Seq(
      "com.typesafe.play" %% "play-ws" % versions.play
    )

  val typesafe =
    Seq(
      "com.typesafe" % "config" % "1.3.1"
    )

  val test =
    Seq(
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )

  val logging =
    Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
    )

  val akka =
    Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.14"
    )

  val akkaHttp = {
    val version = "10.0.0"
    Seq(
      "com.typesafe.akka" %% "akka-http" % version,
      "com.typesafe.akka" %% "akka-http-testkit" % version
    )
  }

  val hbase = {
    val version = "1.2.4"
    Seq(
      "org.apache.hbase" % "hbase-client" % version,
      "org.apache.hbase" % "hbase-common" % version,
      "org.apache.hadoop" % "hadoop-common" % "2.6.5"
    )
  }

  val bigTable =
    Seq(
      "com.google.cloud.bigtable" % "bigtable-hbase-1.2" % "0.9.4",
      "io.netty" % "netty-tcnative-boringssl-static" % "1.1.33.Fork19"
    )

}

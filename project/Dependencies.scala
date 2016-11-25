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
      "com.typesafe" % "config" % "1.3.0"
    )

  val test =
    Seq(
      "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test"
    )

  val logging =
    Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
    )

  val akka =
    Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.12"
    )

  val akkaHttp = {
    val version = "10.0.0"
    Seq(
      "com.typesafe.akka" %% "akka-http" % version,
      "com.typesafe.akka" %% "akka-http-testkit" % version
    )
  }

}

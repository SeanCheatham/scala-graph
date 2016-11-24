import sbt._

object Dependencies {

  val neo4j =
    Seq(
      "org.neo4j.driver" % "neo4j-java-driver" % "1.0.6",
      "org.neo4j" % "neo4j" % "3.0.7"
    )

  val playJson =
    Seq(
      "com.typesafe.play" %% "play-json" % "2.5.10"
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

}

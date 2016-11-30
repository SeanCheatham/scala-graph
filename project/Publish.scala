import sbt.Keys._
import sbt._

object Publish {

  lazy val settings =
    Seq(
      publishMavenStyle := true,
      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value)
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
      },
      publishArtifact in Test := false,
      pomIncludeRepository := { _ => false },
      licenses := Seq("Apache 2.0" -> url("https://opensource.org/licenses/Apache-2.0")),
      homepage := Some(url("http://github.com/seancheatham/scala-graph")),
      pomExtra :=
        <scm>
          <url>git@github.com:seancheatham/scala-graph.git</url>
          <connection>scm:git:git@github.com:scala-graph.git</connection>
          <developerConnection>scm:git:git@github.com:scala-graph.git</developerConnection>
        </scm>
          <developers>
            <developer>
              <id>seancheatham</id>
              <name>Sean Cheatham</name>
              <url>http://github.com/seancheatham</url>
            </developer>
          </developers>
    )

}
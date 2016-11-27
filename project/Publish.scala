import sbt.Keys._
import sbt._

object Publish {

  publishMavenStyle := true

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }

  publishArtifact in Test := false

  pomIncludeRepository := { _ => false }

  licenses := Seq("Apache 2.0" -> url("https://opensource.org/licenses/Apache-2.0"))

  homepage := Some(url("http://github.com/seancheatham/scala-graph"))

  pomExtra :=
    <scm>
      <url>git@github.com:seancheatham/scala-graph.git</url>
      <connection>scm:git:git@github.com:scala-graph.git</connection>
    </scm>
      <developers>
        <developer>
          <id>seancheatham</id>
          <name>Sean Cheatham</name>
          <url>http://github.com/seancheatham</url>
        </developer>
      </developers>

  // This little piece of voodoo courtesy of
  // http://www.cakesolutions.net/teamblogs/publishing-artefacts-to-oss-sonatype-nexus-using-sbt-and-travis-ci
  (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield
    credentials += Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      username,
      password)
    ).getOrElse(credentials ++= Seq())

  import com.typesafe.sbt.SbtPgp._

  pgpPassphrase :=
    Option(System.getenv().get("PGP_PASSPHRASE"))
      .map(_.toCharArray)

  pgpSecretRing := file("local.secring.gpg")

  pgpPublicRing := file("local.pubring.gpg")

}
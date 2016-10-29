lazy val commonSettings =
  Seq(
    organization := "com.seancheatham",
    scalaVersion := "2.11.8",
    libraryDependencies ++=
      Dependencies.playJson ++
        Dependencies.typesafe ++
        Dependencies.test ++
        Dependencies.logging
  )

lazy val graph =
  project
    .in(file("graph"))
    .settings(commonSettings: _*)
    .settings(
      name := "graph",
      libraryDependencies ++= Dependencies.neo4j
    )
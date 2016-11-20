val commonSettings =
  Seq(
    organization := "com.seancheatham",
    scalaVersion := "2.11.8",
    libraryDependencies ++=
      Dependencies.playJson ++
        Dependencies.typesafe ++
        Dependencies.test ++
        Dependencies.logging
  )

lazy val graphCore =
  project
    .in(file("core"))
    .settings(commonSettings: _*)
    .settings(
      name := "graph-core"
    )

lazy val memoryAdapter =
  project
    .in(file("memory"))
    .settings(commonSettings: _*)
    .settings(
      name := "graph-memory-adapter"
    )
    .dependsOn(graphCore)

lazy val graphNeo4jAdapter =
  project
    .in(file("neo4j"))
    .settings(commonSettings: _*)
    .settings(
      name := "graph-neo4j-adapter",
      libraryDependencies ++= Dependencies.neo4j
    )
    .dependsOn(graphCore)

lazy val playGraph =
  project
    .in(file("play"))
    .settings(commonSettings: _*)
    .settings(
      name := "play-graph",
      libraryDependencies ++= Dependencies.playFramework
    )
    .dependsOn(graphCore)
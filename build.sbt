
lazy val commonSettings =
  Seq(
    organization := "com.seancheatham",
    scalaVersion := "2.11.8",
    libraryDependencies ++=
      Dependencies.playJson ++
        Dependencies.typesafe ++
        Dependencies.test ++
        Dependencies.logging
  ) ++ Publish.settings

lazy val root =
  project
    .in(file("."))
    .settings(commonSettings: _*)
    .settings(packagedArtifacts := Map.empty)
    .aggregate(graphCore, memoryAdapter, graphNeo4jAdapter, akkaLayer, akkaAdapter, hBaseAdapter, bigtableAdapter)

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
    .dependsOn(graphCore % "compile->compile;test->test")

lazy val graphNeo4jAdapter =
  project
    .in(file("neo4j"))
    .settings(commonSettings: _*)
    .settings(
      name := "graph-neo4j-adapter",
      libraryDependencies ++= Dependencies.neo4j
    )
    .dependsOn(graphCore % "compile->compile;test->test")

lazy val akkaLayer =
  project
    .in(file("akka"))
    .settings(commonSettings: _*)
    .settings(
      name := "graph-akka-layer",
      resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
      libraryDependencies ++=
        Dependencies.akka ++
          Dependencies.akkaHttp :+
          ("de.heikoseeberger" %% "akka-http-play-json" % "1.10.1")
    )
    // TODO: Adapter by dependency injection
    .dependsOn(graphCore, memoryAdapter, graphNeo4jAdapter)

lazy val akkaAdapter =
  project
    .in(file("akkaAdapter"))
    .settings(commonSettings: _*)
    .settings(
      name := "graph-akka-adapter",
      libraryDependencies ++=
        Dependencies.playWS
    )
    .dependsOn(
      graphCore % "compile->compile;test->test",
      memoryAdapter % "test->test",
      akkaLayer % "test->test"
    )

lazy val hBaseAdapter =
  project
    .in(file("hbase"))
    .settings(commonSettings: _*)
    .settings(
      name := "graph-hbase-adapter",
      libraryDependencies ++=
        Dependencies.hbase
    )
    .dependsOn(
      graphCore % "compile->compile;test->test"
    )

lazy val bigtableAdapter =
  project
    .in(file("bigtable"))
    .settings(commonSettings: _*)
    .settings(
      name := "graph-big-table-adapter",
      libraryDependencies ++=
        Dependencies.hbase ++
          Dependencies.bigTable
    )
    .dependsOn(
      graphCore % "compile->compile;test->test",
      hBaseAdapter
    )



package com.seancheatham.graph.adapters.neo4j

import com.typesafe.config.ConfigFactory
import fixtures.GraphTest
import org.neo4j.driver.v1.AuthTokens

import scala.util.Try

class RemoteNeo4jGraphSpec extends GraphTest {
  private val config =
    ConfigFactory.load()

  private val address =
    Try(config.getString("neo4j.address"))
      .getOrElse("bolt://localhost")

  private val token =
    Try(config.getString("neo4j.auth.user"))
      .map(user =>
        AuthTokens.basic(
          user,
          config.getString("neo4j.auth.password")
        )
      )
      .toOption

  val graph: RemoteNeo4jGraph =
    token.fold(Neo4jGraph.remote(address))(Neo4jGraph.remote(address, _))
}
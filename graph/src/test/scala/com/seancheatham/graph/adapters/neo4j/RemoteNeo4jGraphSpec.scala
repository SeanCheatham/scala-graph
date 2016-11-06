package com.seancheatham.graph.adapters.neo4j

import com.typesafe.config.ConfigFactory
import fixtures.GraphTest
import org.neo4j.driver.v1.AuthTokens

class RemoteNeo4jGraphSpec extends GraphTest({
  val config =
    ConfigFactory.load()
  val address =
    config.getString("neo4j.address")
  val token =
    AuthTokens.basic(
      config.getString("neo4j.auth.user"),
      config.getString("neo4j.auth.password")
    )
  Neo4jGraph(address, token)
})
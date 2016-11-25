package com.seancheatham.graph.adapters.neo4j

import fixtures.GraphTest

class EmbeddedNeo4jGraphSpec extends GraphTest {
  val graph = Neo4jGraph.embedded()
}

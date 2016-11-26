package com.seancheatham.graph.adapters.neo4j

import fixtures.GraphTest

class EmbeddedNeo4jGraphSpec extends GraphTest {
  val graph: EmbeddedNeo4jGraph =
    Neo4jGraph.embedded()

  "Can shutdown" in {
    graph.shutdown()
  }
}

package com.seancheatham.graph.adapters.akka

import fixtures.GraphTest

class RemoteHttpGraphSpec extends GraphTest {
  val graph: RemoteHttpGraph =
    RemoteHttpGraph("localhost", 8080)()

  "The graph" should {
    "shut down" in {
      graph.shutdown()
    }
  }
}
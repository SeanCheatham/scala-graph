package com.seancheatham.graph.adapters.akka

import com.seancheatham.graph.adapters.memory.MutableGraph
import com.seancheatham.graph.akka.http.HttpServer
import fixtures.GraphTest

class RemoteHttpGraphSpec extends GraphTest {
  val graph = RemoteHttpGraph("localhost", 8080)

  "The graph" should {
    "shut down" in {
      graph.shutdown()
    }
  }
}
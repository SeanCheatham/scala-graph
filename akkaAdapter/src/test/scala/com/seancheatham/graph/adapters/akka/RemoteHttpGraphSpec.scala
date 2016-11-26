package com.seancheatham.graph.adapters.akka

import com.seancheatham.graph.adapters.memory.MutableGraph
import com.seancheatham.graph.akka.http.HttpServer
import fixtures.GraphTest

class RemoteHttpGraphSpec extends GraphTest {
  private val port =
    12929

  private val g1 =
    new MutableGraph

  private val server =
    HttpServer(g1, port = port)

  val graph: RemoteHttpGraph =
    RemoteHttpGraph("localhost", port)()

  "The graph" should {
    "shut down" in {
      graph.shutdown()
    }
  }
}
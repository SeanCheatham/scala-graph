package com.seancheatham.graph.akka.http

import com.seancheatham.graph.Graph
import com.seancheatham.graph.adapters.memory.MutableGraph

import scala.io.StdIn

object Application {

  def main(args: Array[String]): Unit = {

    val graph: Graph =
      new MutableGraph

    val host: String =
      "localhost"

    val port: Int =
      8080

    val server =
      HttpServer(graph, host, port)

    println("Press RETURN to stop...")

    StdIn.readLine() // let it run until user presses return

    server.shutdown()

  }

}

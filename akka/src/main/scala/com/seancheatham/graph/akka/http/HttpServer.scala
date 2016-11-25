package com.seancheatham.graph.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.seancheatham.graph.Graph
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

/**
  * A class which instantiates an Akka-HTTP server wrapped around the given graph.  The server will operate
  * at the given host address and port.  The routes for the server provide basic CRUD operations for the
  * nodes and edges in the given graph.  The Graph's API will be used to perform the changes.
  *
  * @param graph The graph to wrap with the HTTP server
  * @param host  The server host to operate on
  * @param port  The server port to bind to
  */
case class HttpServer(graph: Graph,
                      host: String = "localhost",
                      port: Int = 8080) extends LazyLogging {

  // Akka Actor Setup
  implicit val system =
    ActorSystem("http-graph")

  implicit val materializer =
    ActorMaterializer()

  implicit val executionContext: ExecutionContextExecutor =
    system.dispatcher

  // Generate the routes for this server
  val routes: Route =
    Router(graph)

  // Instantiate the server and its binding, and let it start accepting requests
  val binding: Future[ServerBinding] =
    Http().bindAndHandle(routes, host, port)
      .map { binding =>
        logger.info(s"Server online at http://$host:$port/")
        binding
      }

  /**
    * Shuts down this HTTP server by calling the necessary Akka shutdown functions.  This call will block
    * until the server binding completes its shutdown.
    */
  def shutdown(): Unit =
    Await.ready(
      binding
        .flatMap(_.unbind())
        .map(_ => system.terminate()),
      Duration.Inf
    )

}

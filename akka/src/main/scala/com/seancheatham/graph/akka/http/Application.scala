package com.seancheatham.graph.akka.http

import java.util.UUID

import com.seancheatham.graph.Graph
import com.seancheatham.graph.adapters.memory.{ImmutableGraph, MutableGraph}
import com.seancheatham.graph.adapters.neo4j.Neo4jGraph
import com.typesafe.config.ConfigFactory

import scala.io.StdIn
import scala.util.{Failure, Success, Try}

/**
  * The default, command-line instance of the HTTP Server application.  When run,
  * a reference to the configured graph will be formed.  From there, an HTTP Server will
  * wrap the graph, and provide RESTful endpoints to read, write, and mutate the graph.
  *
  * For instructions, please see the documentation by running this application with the "-help" parameter
  */
object Application extends App {

  /**
    * The default binding address for the server
    */
  private final val defaultAddress =
    "localhost"
  /**
    * The default binding port for the server
    */
  private final val defaultPort =
    8080
  /**
    * The default graph type for the server
    */
  private final val defaultGraph =
    "mutable"
  /**
    * Typesafe configuration
    */
  private lazy val config =
    ConfigFactory.load()

  /**
    * Either prints documentation, or initializes a new server and lets it
    * run until the user presses any key in the console
    */
  if (args contains "-help")
    printDocumentation()
  else
    createServer(args) match {
      case Success(server) =>

        println("Press RETURN to stop...")

        // Run the server until there is console input
        StdIn.readLine()

        server.shutdown()
      case Failure(e: IllegalArgumentException) =>
        println(s"Invalid argument provided: ${e.toString}")
      case Failure(e) =>
        throw e
    }

  /**
    * Construct a server from the given arguments
    *
    * @param args The arguments for the application (see the documentation)
    * @return a Try[HttpServer]
    */
  private def createServer(args: Array[String]): Try[HttpServer] =
    constructGraph(args)
      .flatMap(graph =>
        Try(
          args.indexOf("-address") match {
            case -1 =>
              Try(config.getString("graph.http.address"))
                .getOrElse(defaultAddress)
            case i =>
              args.lift(i + 1)
                .getOrElse(throw new IllegalArgumentException("-address"))
          }
        ).flatMap(
          address =>
            Try(
              args.indexOf("-port") match {
                case -1 =>
                  Try(config.getInt("graph.http.port"))
                    .getOrElse(defaultPort)
                case i =>
                  args.lift(i + 1)
                    .map(_.toInt)
                    .getOrElse(throw new IllegalArgumentException("-port"))
              }
            )
              .map(port =>
                HttpServer(graph, address, port)
              )
        )

      )

  /**
    * Construct a graph from the given arguments
    *
    * @param args The arguments for the application (see the documentation)
    * @return a Try[Graph]
    */
  private def constructGraph(args: Array[String]): Try[Graph] =
    Try {
      val gType =
        args.indexOf("-type") match {
          case -1 =>
            Try(config.getString("graph.type"))
              .getOrElse(defaultGraph)
          case i =>
            args.lift(i + 1)
              .getOrElse(throw new IllegalArgumentException("-type"))
        }

      gType match {
        case "mutable" =>
          println(s"Starting mutable in-memory graph")
          new MutableGraph
        case "immutable" =>
          println(s"Starting immutable in-memory graph")
          ImmutableGraph.apply()()
        case "neo4j-embedded" =>
          val directory =
            args.indexOf("-nDir") match {
              case -1 =>
                s"/tmp/${UUID.randomUUID().toString}"
              case i =>
                args.lift(i + 1)
                  .getOrElse(throw new IllegalArgumentException("-nDir"))
            }
          println(s"Starting Embedded Neo4jGraph at $directory")
          Neo4jGraph.embedded(directory)
        case "neo4j-remote" =>
          val address =
            args.indexOf("-nAddress") match {
              case -1 =>
                config.getString("graph.neo4j.address")
              case i =>
                args.lift(i + 1)
                  .getOrElse(throw new IllegalArgumentException("-nAddress"))
            }
          val auth =
            args.indexOf("-nUser") match {
              case -1 =>
                Try(config.getString("graph.neo4j.auth.user"))
                  .toOption
                  .fold[Option[(String, String)]](
                  None
                ) { user =>
                  val password =
                    config.getString("graph.neo4j.auth.password")
                  Some((user, password))
                }
              case i =>
                val user =
                  args(i + 1)
                val password =
                  args.lift(args.indexOf("-nPassword") + 1)
                    .getOrElse(throw new IllegalArgumentException("-nPassword"))
                Some((user, password))
            }
          auth match {
            case Some((user, pass)) =>
              println(s"Connecting to authenticated Neo4j at $address")
              Neo4jGraph.remote(address, user, pass)
            case _ =>
              println(s"Connecting to unauthenticated Neo4j at $address")
              Neo4jGraph.remote(address)
          }
      }
    }

  /**
    * Prints the necessary application usage documentation
    */
  private def printDocumentation(): Unit = {
    println("Graph HTTP Server Usage")
    println("Commands:")
    println("-port: The port to bind the server to (default: 8080)")
    println("-address: The address to bind the server to (default: localhost)")
    println("-type: The type of graph to use (one of: mutable, immutable, neo4j-embedded, neo4j-remote) (default: mutable)")
    println("-nDir: NEO4J-EMBEDDED ONLY, The directory to save the DB to (default: /tmp/{UUID})")
    println("-nAddress: NEO4J-REMOTE ONLY, The address to use when connecting to neo4j")
    println("-nUser: NEO4J-REMOTE ONLY, The (optional) username to use when connecting to neo4j")
    println("-nPassword: NEO4J-REMOTE ONLY, The (optional) password to use when connecting to neo4j")
  }


}

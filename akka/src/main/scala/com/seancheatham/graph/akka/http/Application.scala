package com.seancheatham.graph.akka.http

import java.util.UUID

import com.seancheatham.graph.Graph
import com.seancheatham.graph.adapters.memory.{ImmutableGraph, MutableGraph}
import com.seancheatham.graph.adapters.neo4j.Neo4jGraph
import com.typesafe.config.ConfigFactory
import org.neo4j.driver.v1.{AuthToken, AuthTokens}

import scala.io.StdIn
import scala.util.Try

object Application {

  private lazy val config =
    ConfigFactory.load()

  def main(args: Array[String]): Unit = {

    val graph: Graph =
      constructGraph(args)

    val host: String =
      args.indexOf("-host") match {
        case -1 =>
          Try(config.getString("graph.http.host"))
            .getOrElse("localhost")
        case i =>
          args(i + 1)
      }

    val port: Int =
      args.indexOf("-port") match {
        case -1 =>
          Try(config.getInt("graph.http.port"))
            .getOrElse(8080)
        case i =>
          args(i + 1).toInt
      }

    val server =
      HttpServer(graph, host, port)

    println("Press RETURN to stop...")

    StdIn.readLine() // let it run until user presses return

    server.shutdown()

  }

  def constructGraph(args: Array[String]): Graph = {
    val gType =
      args.indexOf("-type") match {
        case -1 =>
          Try(config.getString("graph.type"))
            .getOrElse("mutable")
        case i =>
          args(i + 1)
      }

    gType match {
      case "mutable" =>
        new MutableGraph
      case "immutable" =>
        ImmutableGraph.apply()()
      case "neo4j-embedded" =>
        val directory =
          args.indexOf("-dir") match {
            case -1 =>
              s"/tmp/${UUID.randomUUID().toString}"
            case i =>
              args(i + 1)
          }
        Neo4jGraph.embedded(directory)
      case "neo4j-remote" =>
        val address =
          args.indexOf("-address") match {
            case -1 =>
              config.getString("graph.neo4j.address")
            case i =>
              args(i + 1)
          }
        val auth =
          args.indexOf("-user") match {
            case -1 =>
              Try(config.getString("graph.neo4j.auth.user"))
                .toOption
                .fold[Option[AuthToken]](
                None
              ) { user =>
                val password =
                  config.getString("graph.neo4j.auth.password")
                Some(AuthTokens.basic(user, password))
              }
            case i =>
              val user =
                args(i + 1)
              val password =
                args(args.indexOf("-password") + 1)
              Some(AuthTokens.basic(user, password))
          }
        auth.fold(
          Neo4jGraph.remote(address)
        )(a =>
          Neo4jGraph.remote(address, a)
        )
    }
  }

}

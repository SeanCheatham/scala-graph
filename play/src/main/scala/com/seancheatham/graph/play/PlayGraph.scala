package com.seancheatham.graph.play

import com.seancheatham.graph.{Edge, Graph, Node}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.routing.sird._
import play.core.server.{NettyServer, ServerConfig}

object PlayGraph {

  /**
    * Given a graph, creates a NettyServer with GET/POST/PUT/DELETE routes to modify and access the graph.
    *
    * @param graph The base graph reference (depending on the graph implementation,
    *              this graph may be GC'd after mutating operations)
    * @param serverConfig A server configuration to apply
    * @return a NettyServer.  Be sure to run NettyServer#start().
    */
  def apply(graph: Graph)
           (implicit serverConfig: ServerConfig = ServerConfig()) = {

    NettyServer.fromRouter(serverConfig)(routes(graph))

  }

  private def routes(graph: Graph): PartialFunction[RequestHeader, Action[AnyContent]] = {

    var g = graph

    val get: PartialFunction[RequestHeader, Action[AnyContent]] = {

      // Default/base route
      case GET(p"/") =>
        Action {
          Results.Ok()
        }

      // Fetch nodes by optional query label, and optional data request body
      case GET(p"/nodes" ? q_?"label=$label") =>
        Action { req =>
          val data =
            req.body.asJson
              .fold(Map.empty[String, JsValue])(_.as[Map[String, JsValue]])

          val nodes =
            Json.toJson(g.getNodes[Node](label, data))

          Results.Ok(nodes)
        }

      // Fetch a node by ID
      case GET(p"/nodes/$id") =>
        Action {
          g.getNode[Node](id)
            .fold(Results.NotFound())(Results.Ok(_))
        }

      // Fetch all edges for a node by ID, with optional query edge label and optional request body data
      case GET(p"/nodes/$id/edges" ? q_?"label=$label") =>
        Action { req =>
          val data =
            req.body.asJson
              .fold(Map.empty[String, JsValue])(_.as[Map[String, JsValue]])

          val node =
            g.getNode[Node](id)

          node.fold(Results.NotFound()) { n =>
            val edges =
              Json.toJson(
                g.getEgressEdges[Edge](n, label, data).toVector ++
                  g.getIngressEdges[Edge](n, label, data)
              )

            Results.Ok(edges)
          }
        }

      // Fetch all EGRESS edges for a node by ID, with optional query edge label and optional request body data
      case GET(p"/nodes/$id/edges/egress" ? q_?"label=$label") =>
        Action { req =>
          val data =
            req.body.asJson
              .fold(Map.empty[String, JsValue])(_.as[Map[String, JsValue]])

          val node =
            g.getNode[Node](id)

          node.fold(Results.NotFound()) { n =>
            val edges =
              Json.toJson(g.getEgressEdges[Edge](n, label, data))

            Results.Ok(edges)
          }
        }

      // Fetch all INGRESS edges for a node by ID, with optional query edge label and optional request body data
      case GET(p"/nodes/$id/edges/ingress" ? q_?"label=$label") =>
        Action { req =>
          val data =
            req.body.asJson
              .fold(Map.empty[String, JsValue])(_.as[Map[String, JsValue]])

          val node =
            g.getNode[Node](id)

          node.fold(Results.NotFound()) { n =>
            val edges =
              Json.toJson(g.getIngressEdges[Edge](n, label, data))

            Results.Ok(edges)
          }
        }

      // Fetch edges by optional query label, and optional data request body
      case GET(p"/edges" ? q_?"label=$label") =>
        Action { req =>
          val data =
            req.body.asJson
              .fold(Map.empty[String, JsValue])(_.as[Map[String, JsValue]])

          val edges =
            Json.toJson(g.getEdges[Edge](label, data))

          Results.Ok(edges)
        }
    }

    val post: PartialFunction[RequestHeader, Action[AnyContent]] = {

      // Create a new node
      case POST(p"/nodes" ? q"label=$label") =>
        Action { req =>
          val data =
            req.body.asJson
              .fold(Map.empty[String, JsValue])(_.as[Map[String, JsValue]])

          val node = g.addNode[Node](label, data)

          g = node.graph

          Results.Ok(Json.toJson(node))
        }

      // Create a new edge from node1 to node2
      case POST(p"/nodes/$_1/to/$_2" ? q"label=$label") =>
        Action { req =>
          val data =
            req.body.asJson
              .fold(Map.empty[String, JsValue])(_.as[Map[String, JsValue]])

          val node1 = g.getNode[Node](_1)
          val node2 = g.getNode[Node](_2)

          if (node1.isEmpty || node2.isEmpty)
            Results.NotFound()
          else {
            val edge = g.addEdge[Edge](label, node1.get, node2.get, data)
            g = edge.graph
            Results.Ok(Json.toJson(edge))
          }
        }
    }

    val put: PartialFunction[RequestHeader, Action[AnyContent]] = {

      // Update a node
      case PUT(p"/nodes/$id") =>
        Action { req =>
          val data =
            req.body.asJson
              .fold(Map.empty[String, JsValue])(_.as[Map[String, JsValue]])

          val node = g.getNode[Node](id)

          node.fold(Results.NotFound()) {
            n =>
              val updatedNode = g.updateNode[Node](n)(data.toSeq: _*)
              g = updatedNode.graph
              Results.Ok(Json.toJson(updatedNode))
          }
        }

      // Update an edge
      case PUT(p"/edges/$id") =>
        Action { req =>
          val data =
            req.body.asJson
              .fold(Map.empty[String, JsValue])(_.as[Map[String, JsValue]])

          val edge = g.getEdge[Edge](id)

          edge.fold(Results.NotFound()) {
            e =>
              val updatedEdge = g.updateEdge[Edge](e)(data.toSeq: _*)
              g = updatedEdge.graph
              Results.Ok(Json.toJson(updatedEdge))
          }
        }
    }

    val delete = {

      // Delete a node by ID
      case DELETE(p"/nodes/$id") =>
        Action {
          val node = g.getNode[Node](id)

          node.fold(Results.NotFound()) {
            n =>
              g = g.removeNode(n)
              Results.NoContent
          }
        }

      // Delete an edge by ID
      case DELETE(p"/edges/$id") =>
        Action {
          val edge = g.getEdge[Edge](id)

          edge.fold(Results.NotFound()) {
            e =>
              g = g.removeEdge(e)
              Results.NoContent
          }
        }

      // Delete all nodes matching the given optional label and data
      case DELETE(p"/nodes" ? q_?"label=$label") =>
        Action { req =>
          val data =
            req.body.asJson
              .fold(Map.empty[String, JsValue])(_.as[Map[String, JsValue]])

          g = g.removeNodes(label, data)

          Results.NoContent
        }
    }

    get
      .orElse(post)
      .orElse(put)
      .orElse(delete)
      .orElse {
        // By default, just 404
        case _: RequestHeader =>
          Action(Results.NotFound())
      }
  }

}
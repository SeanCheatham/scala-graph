package com.seancheatham.graph.akka.http

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import com.seancheatham.graph.{Edge, Graph, Node}
import play.api.libs.json.{JsValue, Json}

/**
  * Helper object used for determining routes for Graph HTTP Servers
  */
object Router {

  /**
    * The documentation provided to the user at the base "/" path
    */
  final private val documentation: String =
    """<h1>HTTP Graph Layer</h1>
      |<h2>API Usage</h2>
      |<h3>Routes</h3>
      |<p></p>
      |<h3>Models</h3>
      |<p>...</p>
    """.stripMargin

  /**
    * Generates a router with basic CRUD functionality for graph operations.
    *
    * @param graph The graph to influence/mutate/read
    * @return a router in the Akka high-level routing DSL
    */
  def apply(graph: Graph): Route = {
    import CustomMarshallers._
    import StatusCodes._
    import akka.http.scaladsl.model.HttpEntity
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport.playJsonMarshaller

    // Keep a variable graph reference; this will be updated every time a mutating request is made
    var g = graph

    val base =
      pathSingleSlash(
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, documentation))
      )

    // Provides routing for reading/updating/deleting a specific node.
    // Also provides routes for accessing the node's edges
    val nodeRoutes =
    path("nodes" / Segment) { id =>
      val maybeNode = g.getNode[Node](id)
      // If the node couldn't be found, 404
      maybeNode.fold[Route](complete(NotFound))(node =>
        // Get a node
        get(complete(Json.toJson(node))) ~
          // Update a node
          put(
            optionalEntity(as[Map[String, JsValue]]) {
              data =>
                val updatedNode = g.updateNode(node)((data getOrElse Map.empty).toSeq: _*)
                g = updatedNode.graph
                complete(Json.toJson(updatedNode))
            }
          ) ~
          // Delete a node
          delete {
            g = g.removeNode(node)
            complete(NoContent)
          }
      )
    } ~
      get {
        // Aggregate all of this node's edges
        path("nodes" / Segment / "edges") { id =>
          val maybeNode = g.getNode[Node](id)
          // If the node couldn't be found, 404
          maybeNode.fold[Route](complete(NotFound))(node =>
            parameter("label", 'label.?) { (_, label) =>
              optionalEntity(as[Map[String, JsValue]]) {
                data =>
                  val d =
                    data getOrElse Map.empty
                  val edges =
                    g.getEgressEdges[Edge](node, label, d).toIterator ++
                      g.getIngressEdges[Edge](node, label, d)
                  complete(edges.map(Json.toJson(_)))
              }
            }
          )
        } ~
          // Get this node's outgoing/egress edges
          path("nodes" / Segment / "edges" / "egress") { id =>
            val maybeNode = g.getNode[Node](id)
            maybeNode.fold[Route](complete(NotFound))(node =>
              parameter("label", 'label.?) { (_, label) =>
                optionalEntity(as[Map[String, JsValue]]) {
                  data =>
                    val edges =
                      g.getEgressEdges[Edge](node, label, data getOrElse Map.empty).toIterator
                    complete(edges.map(Json.toJson(_)))
                }
              }
            )
          } ~
          // Get this node's incoming/ingress edges
          path("nodes" / Segment / "edges" / "ingress") { id =>
            val maybeNode = g.getNode[Node](id)
            maybeNode.fold[Route](complete(NotFound))(node =>
              parameter("label", 'label.?) { (_, label) =>
                optionalEntity(as[Map[String, JsValue]]) {
                  data =>
                    val edges =
                      g.getIngressEdges[Edge](node, label, data getOrElse Map.empty).toIterator
                    complete(edges.map(Json.toJson(_)))
                }
              }
            )
          }
      }

    // Routes for working with collections of nodes
    val nodesRoutes =
      path("nodes")(
        // Get all nodes by label? and data?
        get(
          parameter("label".?)(label =>
            optionalEntity(as[Map[String, JsValue]]) {
              data =>
                val nodes =
                  g.getNodes[Node](label, data getOrElse Map.empty)
                complete(nodes.map(Json.toJson(_)))
            }
          )
        ) ~
          // Create a new node
          post(
            parameter("label")(label =>
              optionalEntity(as[Map[String, JsValue]]) {
                data =>
                  val node =
                    g.addNode[Node](label, data getOrElse Map.empty)
                  g = node.graph
                  complete(Json.toJson(node))
              }
            )
          )
      )

    // Routes for operating on a specific edge
    val edgeRoutes =
      path("edges" / Segment) { id =>
        val maybeEdge = g.getEdge[Edge](id)
        // If the edge could not be found, 404
        maybeEdge.fold[Route](complete(NotFound))(edge =>
          // Get an edge
          get(complete(Json.toJson(edge))) ~
            // Update an edge
            put(
              optionalEntity(as[Map[String, JsValue]]) {
                data =>
                  val updatedEdge = g.updateEdge(edge)((data getOrElse Map.empty).toSeq: _*)
                  g = updatedEdge.graph
                  complete(Json.toJson(updatedEdge))
              }
            ) ~
            // Delete an edge
            delete {
              g = g.removeEdge(edge)
              complete(NoContent)
            }
        )
      }

    // Routes to operate on collections of edges
    val edgesRoutes =
      path("edges")(
        // Get all edges by label and data
        get(
          parameter("label".?)(label =>
            optionalEntity(as[Map[String, JsValue]]) {
              data =>
                val edges =
                  g.getEdges[Edge](label, data getOrElse Map.empty)
                complete(edges.map(Json.toJson(_)))
            }
          )
        )
      )

    // Routes to operate on edges filtered by _1 and _2
    val edgeFilterRoutes =
      path("nodes" / Segment / "to" / Segment)((a, b) =>
        post(
          parameter("label")(label =>
            optionalEntity(as[Map[String, JsValue]]) {
              data =>
                val node1 =
                  g.getNode[Node](a)
                val node2 =
                  g.getNode[Node](b)

                if(node1.isEmpty)
                  complete(NotFound)
                else if(node2.isEmpty)
                  complete(NotFound)
                else {
                  val edge = g.addEdge[Edge](label, node1.get, node2.get, data getOrElse Map.empty)
                  g = edge.graph
                  complete(Json.toJson(edge))
                }
            }
          )
        )
      )

    base ~
      nodesRoutes ~
      edgesRoutes ~
      edgeFilterRoutes ~
      nodeRoutes ~
      edgeRoutes

  }

}

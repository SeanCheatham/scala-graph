package com.seancheatham.graph.adapters.neo4j

import com.seancheatham.graph.{Edge, Graph, Node}
import com.typesafe.config.ConfigFactory
import org.neo4j.driver.v1.AuthTokens
import org.scalatest.WordSpec
import play.api.libs.json.{JsBoolean, JsNumber, JsString, Json}

class Neo4jGraphSpec extends WordSpec {


  "A remote Neo4j Graph Database" can
    graphTest {
      val config =
        ConfigFactory.load()
      val address =
        config.getString("neo4j.address")
      val token =
        AuthTokens.basic(
          config.getString("neo4j.auth.user"),
          config.getString("neo4j.auth.password")
        )
      Neo4jGraph(address, token)
    }

  "An embedded Neo4j Graph Database" can
    graphTest(
      Neo4jGraph.embedded()
    )

  def graphTest(graph: Graph) = {

    lazy val node1 =
      graph.addNode[Node]("TEST", Map("name" -> JsString("Foo")))

    "create a node" in {
      assert(node1.label == "TEST")

      assert(node1.data("name").as[String] == "Foo")
    }

    "get a node by ID" in {
      val alsoNode1 =
        graph.getNode[Node](node1.id).get

      assert(alsoNode1.label == "TEST")

      assert(alsoNode1.data("name").as[String] == "Foo")

      assert(node1.id == alsoNode1.id)
    }

    "get a node by match" in {
      val alsoNode1 =
        graph.getNodes[Node](Some("TEST"), Map("name" -> JsString("Foo"))).toIterator.next()

      assert(alsoNode1.label == "TEST")

      assert(alsoNode1.data("name").as[String] == "Foo")

      assert(node1.id == alsoNode1.id)
    }

    lazy val node2 =
      graph.addNode[Node]("TEST", Map("name" -> JsString("Bar")))

    "create another node" in {
      assert(node2.label == "TEST")

      assert(node2.data("name").as[String] == "Bar")
    }

    lazy val edge1 =
      graph.addEdge[Edge](node1, node2, "TESTEDGE", Map("weight" -> Json.toJson(1.5)))

    "connect two nodes" in {
      assert(edge1.label == "TESTEDGE")

      assert(edge1.data("weight").as[Float] == 1.5)
    }

    "path from node1 to node3" in {

      val node3 =
        graph.addNode[Node]("TEST", Map("name" -> JsString("Baz")))

      val edge2 =
        graph.addEdge[Edge](node2, node3, "TESTEDGE", Map("weight" -> Json.toJson(5)))

      val paths =
        graph.pathsTo(node1, node3, Seq("TEST"), Seq("TESTEDGE"))
          .toVector

      assert(paths.head.startNode.id == node1.id)

      assert(paths.head.nodes.exists(_.id == node2.id))

      assert(paths.head.endNode.id == node3.id)

    }

    lazy val node1OutgoingEdges =
      graph.getEgressEdges[Edge](node1).toVector

    "get the outgoing edges for node1" in {
      assert(node1OutgoingEdges.size == 1)
      assert(node1OutgoingEdges.head._2.id == node2.id)
      assert(node1OutgoingEdges.head.data("weight").as[Float] == 1.5)
      assert(node1OutgoingEdges.head.label == "TESTEDGE")
    }

    lazy val node2IncomingEdges =
      graph.getIngressEdges[Edge](node2).toVector

    "get the incoming edges for node2" in {
      assert(node2IncomingEdges.size == 1)
      assert(node2IncomingEdges.head._1.id == node1.id)
      assert(node2IncomingEdges.head.data("weight").as[Float] == 1.5)
      assert(node2IncomingEdges.head.label == "TESTEDGE")
    }

    lazy val updatedNode1 =
      graph.updateNode(node1)("name" -> JsString("Potato"), "other" -> JsBoolean(true))

    "update a node" in {
      assert(updatedNode1.id == node1.id)
      assert(updatedNode1.data("name").as[String] == "Potato")
      assert(updatedNode1.data("other").as[Boolean])
    }

    lazy val updatedEdge1 =
      graph.updateEdge(edge1)("weight" -> JsNumber(5.9))

    "update an edge" in {
      assert(updatedEdge1.id == edge1.id)
      assert(updatedEdge1.data("weight").as[Float].toInt == 5)
    }

    "remove an edge" in {
      graph.removeEdge(edge1)

      val newEdges =
        graph.getIngressEdges[Edge](node2).toVector

      assert(newEdges.isEmpty)
    }

    "remove a node" in {
      graph.removeNode(node1)

      val newNodes =
        graph.getNodes[Node](Some("TEST")).toVector

      assert(newNodes.size == 2)
      assert(newNodes.head.id == node2.id)
    }

    "remove nodes" in {
      graph.removeNodes(Some("TEST"))

      val newNodes =
        graph.getNodes[Node](Some("TEST")).toVector

      assert(newNodes.isEmpty)
    }

  }

}

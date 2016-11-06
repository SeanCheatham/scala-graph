package fixtures

import com.seancheatham.graph.{Edge, Graph, Node}
import org.scalatest.WordSpec
import play.api.libs.json._

abstract class GraphTest(graph: Graph) extends WordSpec {

  lazy val node1 =
    graph.addNode[Node]("TEST", Map("name" -> JsString("Foo")))

  "create a node" in {
    assert(node1.label == "TEST")

    assert(node1.data("name").as[String] == "Foo")
  }

  "get a node by ID" in {
    val alsoNode1 =
      node1.graph.getNode[Node](node1.id).get

    assert(alsoNode1.label == "TEST")

    assert(alsoNode1.data("name").as[String] == "Foo")

    assert(node1.id == alsoNode1.id)
  }

  "get a node by match" in {
    val alsoNode1 =
      node1.graph.getNodes[Node](Some("TEST"), Map("name" -> JsString("Foo"))).toIterator.next()

    assert(alsoNode1.label == "TEST")

    assert(alsoNode1.data("name").as[String] == "Foo")

    assert(node1.id == alsoNode1.id)
  }

  lazy val node2 =
    node1.graph.addNode[Node]("TEST", Map("name" -> JsString("Bar")))

  "create another node" in {
    assert(node2.label == "TEST")

    assert(node2.data("name").as[String] == "Bar")
  }

  lazy val edge1 =
    node2.graph.addEdge[Edge]("TESTEDGE", node1, node2, Map("weight" -> Json.toJson(1.5)))

  "connect two nodes" in {
    assert(edge1.label == "TESTEDGE")

    assert(edge1.data("weight").as[Float] == 1.5)
  }

  lazy val node1OutgoingEdges =
    edge1.graph.getEgressEdges[Edge](node1).toVector

  "get the outgoing edges for node1" in {
    assert(node1OutgoingEdges.size == 1)
    assert(node1OutgoingEdges.head._2.id == node2.id)
    assert(node1OutgoingEdges.head.data("weight").as[Float] == 1.5)
    assert(node1OutgoingEdges.head.label == "TESTEDGE")
  }

  lazy val node2IncomingEdges =
    edge1.graph.getIngressEdges[Edge](node2).toVector

  "get the incoming edges for node2" in {
    assert(node2IncomingEdges.size == 1)
    assert(node2IncomingEdges.head._1.id == node1.id)
    assert(node2IncomingEdges.head.data("weight").as[Float] == 1.5)
    assert(node2IncomingEdges.head.label == "TESTEDGE")
  }

  "path from node1 to node5" in {

    val node3 =
      edge1.graph.addNode[Node]("TEST", Map("name" -> JsString("a")))

    val node4 =
      node3.graph.addNode[Node]("OTHER_TEST", Map("name" -> JsString("b")))

    val node5 =
      node4.graph.addNode[Node]("TEST", Map("name" -> JsString("c")))

    import com.seancheatham.graph.Edge.NodeEdgeSyntax

    val withEdges =
      node5.graph
        .addEdge[Edge]("OTHER_TESTEDGE", node1, node5, Map.empty[String, JsValue]).graph
        .addEdge[Edge](node1 -"TESTEDGE"-> node4, Map("weight" -> Json.toJson(5))).graph
        .addEdge[Edge](node2 -"TESTEDGE"-> node3, Map("weight" -> Json.toJson(5))).graph
        .addEdge[Edge](node3 -"TESTEDGE"-> node5, Map("weight" -> Json.toJson(5))).graph

    val paths =
      withEdges.pathsTo(node1, node5, Seq("TEST"), Seq("TESTEDGE"))
        .toVector

    assert(paths.size == 1)

    val path =
      paths.head

    assert(path.nodes.map(_.id) == Vector(node1.id, node2.id, node3.id, node5.id))

  }

  lazy val updatedNode1 =
    edge1.graph.updateNode(node1)("name" -> JsString("Potato"), "other" -> JsBoolean(true))

  "update a node" in {
    assert(updatedNode1.id == node1.id)
    assert(updatedNode1.data("name").as[String] == "Potato")
    assert(updatedNode1.data("other").as[Boolean])
  }

  lazy val updatedEdge1 =
    updatedNode1.graph.updateEdge(edge1)("weight" -> JsNumber(5.9))

  "update an edge" in {
    assert(updatedEdge1.id == edge1.id)
    assert(updatedEdge1.data("weight").as[Float].toInt == 5)
  }

  lazy val oldEdgeSize =
    updatedEdge1.graph.getIngressEdges[Edge](node2).toVector.size

  lazy val withEdgeRemoved =
    updatedEdge1.graph.removeEdge(edge1)

  "remove an edge" in {
    // Force evaluation
    oldEdgeSize

    val newEdgeSize =
      withEdgeRemoved.getIngressEdges[Edge](node2).toVector.size

    assert(newEdgeSize == (oldEdgeSize - 1))
  }

  lazy val withNodeRemoved =
    withEdgeRemoved.removeNode(node1)

  "remove a node" in {

    val nodes =
      withEdgeRemoved.getNodes[Node](Some("TEST")).toVector

    val newNodes =
      withNodeRemoved.getNodes[Node](Some("TEST")).toVector

    assert(newNodes.size == (nodes.size - 1))

    assert(withNodeRemoved.getNode[Node](node1.id).isEmpty)
  }

  "remove nodes" in {
    val withNodesRemoved =
      withNodeRemoved.removeNodes()

    val newNodes =
      withNodesRemoved.getNodes[Node]().toVector

    assert(newNodes.isEmpty)
  }

}
package com.seancheatham.graph.adapters.memory

import com.seancheatham.graph.{Edge, Node}
import fixtures.GraphTest
import play.api.libs.json._

class MutableGraphSpec extends GraphTest {
  val graph = new MutableGraph()

  "A graph" can {
    val g1 =
      new MutableGraph()
    val n1 = g1.addNode[Node]("1", "test", Map.empty[String, JsValue])
    val n2 = g1.addNode[Node]("2", "test", Map("foo" -> JsString("bar")))
    val n3 = g1.addNode[Node]("3", "test", Map("baz" -> JsNumber(5)))
    g1.addEdge[Edge]("test", n1, n2, Map("weight" -> JsNumber(3)))
    g1.addEdge[Edge]("test", n2, n3, Map("weight" -> JsNumber(3)))

    lazy val json = Json.toJson(g1)

    "be exported to JSON" in {
      import com.seancheatham.graph.Node.reads
      val nodeConstructs = (json \ "nodes").as[Seq[Node.Construct]]
      assert(nodeConstructs.exists(_._1 == "1"))
      assert(nodeConstructs.exists(_._1 == "2"))
      assert(nodeConstructs.exists(_._1 == "3"))
      assert(nodeConstructs.exists(c => c._1 == "2" && c._3("foo").as[String] == "bar"))
      assert(nodeConstructs.exists(c => c._1 == "3" && c._3("baz").as[Int] == 5))

      val edgeConstructs = (json \ "edges").as[Seq[JsObject]].map(_.as[Edge.Construct](Edge.reads(g1)))
      assert(edgeConstructs.exists(e => e._3.id == "1" && e._4.id == "2"))
      assert(edgeConstructs.exists(e => e._3.id == "2" && e._4.id == "3"))
      assert(edgeConstructs.exists(e => e._3.id == "1" && e._4.id == "2" && e._5("weight").as[Int] == 3))
      assert(edgeConstructs.exists(e => e._3.id == "2" && e._4.id == "3" && e._5("weight").as[Int] == 3))
    }

    "be imported from JSON" in {
      val g2 = json.as[MutableGraph]
      assert(g1.getNodes[Node]() forall (n =>
        g2.getNode[Node](n.id).fold(false)(_.data == n.data))
      )
      assert(g1.getEdges[Edge]() forall (e =>
        g2.getEdge[Edge](e.id).fold(false)(e2 =>
          e2._1.id == e._1.id && e2._2.id == e._2.id && e2.data == e.data)
        )
      )
    }

  }
}
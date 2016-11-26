package com.seancheatham.graph.akka.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.seancheatham.graph.adapters.memory.MutableGraph
import com.seancheatham.graph.akka.http.CustomMarshallers._
import com.seancheatham.graph.{Edge, Node}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json._

class HttpServerSpec extends WordSpec with Matchers with ScalatestRouteTest {

  implicit val graph =
    new MutableGraph

  val server =
    HttpServer(graph, port = 12319)

  val defaultLabel =
    "test"

  "An Akka HTTP Server backed by a Mutable Graph" can {
    "POST /nodes" in {
      val data =
        Json.obj("name" -> "foo")
      Post(s"/nodes?label=$defaultLabel", data) ~>
        server.routes ~> check {
        val node =
          Node.fromJson(responseAs[JsObject])

        status shouldEqual StatusCodes.OK
        node.label shouldEqual defaultLabel
        node.data.foreach(kv => node.data(kv._1) shouldEqual kv._2)
        graph.getNode[Node](node.id).get.id shouldEqual node.id
      }
    }

    "GET /nodes" in {
      val graphNodes =
        graph.getNodes[Node]().toSeq
      Get(s"/nodes") ~>
        server.routes ~> check {
        val nodes =
          entityAs[JsArray].value
            .map(_.as[JsObject])
            .map(Node.fromJson)

        status shouldEqual StatusCodes.OK

        nodes.foreach {
          node =>
            val graphNode =
              graphNodes.find(_.id == node.id).get
            node.label shouldEqual graphNode.label
            node.data.foreach(kv => node.data(kv._1) shouldEqual kv._2)
            graphNode.id shouldEqual node.id
        }
      }
    }

    "GET /nodes/{id}" in {
      val graphNode =
        graph.getNodes[Node]().toSeq.head
      Get(s"/nodes/${graphNode.id}") ~>
        server.routes ~> check {
        val node =
          Node.fromJson(responseAs[JsObject])

        status shouldEqual StatusCodes.OK
        node.label shouldEqual graphNode.label
        node.data.foreach(kv => node.data(kv._1) shouldEqual kv._2)
        graphNode.id shouldEqual node.id
      }
    }

    "PUT /nodes/{id}" in {
      val originalNode =
        graph.getNodes[Node]().toVector.head
      val newData =
        Json.obj("name" -> "bar")
      Put(s"/nodes/${originalNode.id}", newData) ~>
        server.routes ~> check {
        val node =
          Node.fromJson(responseAs[JsObject])

        status shouldEqual StatusCodes.OK
        node.label shouldEqual defaultLabel
        node.data("name").as[String] shouldEqual "bar"
        graph.getNode[Node](node.id).get.id shouldEqual originalNode.id
      }
    }

    // Create another node
    lazy val bazNode =
      graph.addNode[Node](defaultLabel, Map("name" -> JsString("baz")))

    lazy val barNode =
      graph.getNodes[Node](data = Map("name" -> JsString("bar"))).toSeq.head

    "POST /nodes/{_1}/to/{_2}" in {
      val edgeData =
        Json.obj(
          "weight" -> 1.4
        )
      Post(s"/nodes/${barNode.id}/to/${bazNode.id}?label=$defaultLabel", edgeData) ~>
        server.routes ~> check {
        val edge =
          Edge.fromJson(responseAs[JsObject], barNode, bazNode)

        status shouldEqual StatusCodes.OK
        edge.label shouldEqual defaultLabel
        edgeData.value.foreach(kv => edge.data(kv._1) shouldEqual kv._2)
        graph.getEdge[Edge](edge.id).get.id shouldEqual edge.id
      }
    }

    "PUT /edges/{id}" in {
      val edgeData =
        Json.obj(
          "weight" -> 3.3
        )
      val graphEdge =
        barNode.egressEdges[Edge]().toSeq.head
      Put(s"/edges/${graphEdge.id}", edgeData) ~>
        server.routes ~> check {
        val edge =
          Edge.fromJson(responseAs[JsObject], barNode, bazNode)

        status shouldEqual StatusCodes.OK
        edge.label shouldEqual defaultLabel
        // Covert to Int to avoid Float equality issues
        edge.data("weight").as[Float].toInt shouldEqual 3
        graph.getEdge[Edge](edge.id).get.id shouldEqual edge.id
      }
    }

    "GET /nodes/{id}/edges" in {
      val graphEdges =
        barNode.ingressEdges[Edge]().toVector ++
          barNode.egressEdges[Edge]()
      Get(s"/nodes/${barNode.id}/edges") ~>
        server.routes ~> check {
        val edges =
          entityAs[JsArray].value
            .map(_.as[JsObject])
            .map(Edge.fromJson)

        status shouldEqual StatusCodes.OK

        edges.foreach {
          edge =>
            val graphEdge =
              graphEdges.find(_.id == edge.id).get
            edge.label shouldEqual graphEdge.label
            edge.data.foreach(kv => edge.data(kv._1) shouldEqual kv._2)
            graphEdge.id shouldEqual edge.id
        }
      }
    }

    "GET /nodes/{id}/edges/ingress" in {
      val graphEdges =
        barNode.ingressEdges[Edge]().toVector
      Get(s"/nodes/${barNode.id}/edges/ingress") ~>
        server.routes ~> check {
        val edges =
          entityAs[JsArray].value
            .map(_.as[JsObject])
            .map(Edge.fromJson)

        status shouldEqual StatusCodes.OK

        edges.foreach {
          edge =>
            val graphEdge =
              graphEdges.find(_.id == edge.id).get
            edge.label shouldEqual graphEdge.label
            edge.data.foreach(kv => edge.data(kv._1) shouldEqual kv._2)
            graphEdge.id shouldEqual edge.id
        }
      }
    }

    "GET /nodes/{id}/edges/egress" in {
      val graphEdges =
        barNode.egressEdges[Edge]().toVector
      Get(s"/nodes/${barNode.id}/edges/egress") ~>
        server.routes ~> check {
        val edges =
          entityAs[JsArray].value
            .map(_.as[JsObject])
            .map(Edge.fromJson)

        status shouldEqual StatusCodes.OK

        edges.foreach {
          edge =>
            val graphEdge =
              graphEdges.find(_.id == edge.id).get
            edge.label shouldEqual graphEdge.label
            edge.data.foreach(kv => edge.data(kv._1) shouldEqual kv._2)
            graphEdge.id shouldEqual edge.id
        }
      }
    }

    "GET /edges/{id}" in {
      val graphEdge =
        graph.getEdges[Edge]().toSeq.head
      Get(s"/edges/${graphEdge.id}") ~>
        server.routes ~> check {
        val edge =
          Edge.fromJson(responseAs[JsObject], graph.getNode[Node] _, graph.getNode[Node] _)

        status shouldEqual StatusCodes.OK
        edge.label shouldEqual graphEdge.label
        edge.data.foreach(kv => edge.data(kv._1) shouldEqual kv._2)
        graphEdge.id shouldEqual edge.id
      }
    }

    "DELETE /edges/{id}" in {
      val graphEdge =
        graph.getEdges[Edge]().toSeq.head
      Delete(s"/edges/${graphEdge.id}") ~>
        server.routes ~> check {
        status shouldEqual StatusCodes.NoContent
        graph.getEdges[Edge]().toSeq.isEmpty === true
      }
    }

    "DELETE /nodes/{id}" in {
      Delete(s"/nodes/${barNode.id}") ~>
        server.routes ~> check {
        status shouldEqual StatusCodes.NoContent
        graph.getNode[Node](barNode.id).isEmpty === true
      }
    }

    "Shut down" in {
      server.shutdown()
      assert(true)
    }
  }

}

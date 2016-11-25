package com.seancheatham.graph.akka.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.seancheatham.graph.{Edge, Node}
import com.seancheatham.graph.adapters.memory.MutableGraph
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsObject, JsValue, Json}
import com.seancheatham.graph.akka.http.CustomMarshallers._

class HttpServerSpec extends WordSpec with Matchers with ScalatestRouteTest {

  implicit val graph =
    new MutableGraph

  val server =
    HttpServer(graph)

  val defaultLabel =
    "test"

  "An Akka HTTP Server backed by a Mutable Graph" can {
    "create a node" in {
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

    "get a node" in {
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
    "update a node" in {
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

    "get a node's edges" in {
      val graphNode =
        graph.getNodes[Node]().toSeq.head
      val graphEdges =
        graphNode.ingressEdges[Edge]().toVector ++
          graphNode.egressEdges[Edge]()
      Get(s"/nodes/${graphNode.id}/edges") ~>
        server.routes ~> check {
        val edges =
          entityAs[Iterator[JsValue]].toVector
            .map(_.as[JsObject])
            .map(Edge.fromJson(_, graph.getNode[Node] _, graph.getNode[Node] _))

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

    "get a node's ingress edges" in {
      val graphNode =
        graph.getNodes[Node]().toSeq.head
      val graphEdges =
        graphNode.ingressEdges[Edge]().toVector
      Get(s"/nodes/${graphNode.id}/edges/ingress") ~>
        server.routes ~> check {
        val edges =
          entityAs[Iterator[JsValue]].toVector
            .map(_.as[JsObject])
            .map(Edge.fromJson(_, graph.getNode[Node] _, graph.getNode[Node] _))

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

    "get a node's egress edges" in {
      val graphNode =
        graph.getNodes[Node]().toSeq.head
      val graphEdges =
        graphNode.egressEdges[Edge]().toVector
      Get(s"/nodes/${graphNode.id}/edges/egress") ~>
        server.routes ~> check {
        val edges =
          entityAs[Iterator[JsValue]].toVector
            .map(_.as[JsObject])
            .map(Edge.fromJson(_, graph.getNode[Node] _, graph.getNode[Node] _))

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

    "get all nodes" in {
      val graphNodes =
        graph.getNodes[Node]().toSeq
      Get(s"/nodes") ~>
        server.routes ~> check {
        val nodes =
          entityAs[Iterator[JsValue]].toVector
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

  }

}

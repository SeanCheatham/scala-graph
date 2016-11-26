package com.seancheatham.graph.adapters.akka

import java.io.InputStream

import akka.actor.{ActorSystem, Terminated}
import akka.stream.ActorMaterializer
import com.seancheatham.graph.{Edge, Graph, Node, Path}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.StreamedResponse

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

case class RemoteHttpGraph(address: String,
                           port: Int)
                          (override implicit val nodeFactory: Node.Factory = Node.defaultFactory,
                           override implicit val edgeFactory: Edge.Factory = Edge.defaultFactory) extends Graph with LazyLogging {

  val baseUri =
    s"http://$address:$port"

  import play.api.libs.ws.ahc.AhcWSClient
  import RemoteHttpGraph._

  implicit val actorSystem: ActorSystem =
    ActorSystem()

  implicit val materializer: ActorMaterializer =
    ActorMaterializer()

  implicit val executionContext: ExecutionContextExecutor =
    actorSystem.dispatcher

  private val client =
    AhcWSClient()

  def shutdown(): Future[Terminated] = {
    client.close()
    actorSystem.terminate()
  }

  def addNode[N <: Node](label: String, data: Map[String, JsValue]): N =
    path(s"/nodes")
      .withQueryString("label" -> label)
      .post(JsObject(data))
      .map(r => Node.fromJson(r.json.as[JsObject]).asInstanceOf[N])
      .awaitForever

  def addEdge[E <: Edge](label: String, _1: Node, _2: Node, data: Map[String, JsValue]): E =
    path(s"/nodes/${_1.id}/to/${_2.id}")
      .withQueryString("label" -> label)
      .post(JsObject(data))
      .map(r => Edge.fromJson(r.json.as[JsObject], _1, _2).asInstanceOf[E])
      .awaitForever

  def getNode[N <: Node](id: String): Option[N] =
    path(s"/nodes/$id")
      .get()
      .map {
        case r if r.status == 200 =>
          Some(Node.fromJson(r.json.as[JsObject]).asInstanceOf[N])
        case _ =>
          None
      }
      .awaitForever

  def getEdge[E <: Edge](id: String): Option[E] =
    path(s"/edges/$id")
      .get()
      .map {
        case r if r.status == 200 =>
          Some(Edge.fromJson(r.json.as[JsObject]).asInstanceOf[E])
        case _ =>
          None
      }
      .awaitForever

  def getEdges[E <: Edge](label: Option[String], data: Map[String, JsValue]): TraversableOnce[E] =
    getEdges[E]("/edges", label, data)

  private def getEdges[E <: Edge](p: String,
                                  label: Option[String],
                                  data: Map[String, JsValue]) =
    label.fold(path(p))(l => path(p).withQueryString("label" -> l))
      .withBody(JsObject(data))
      .withMethod("GET")
      .stream()
      .flatMap(streamToIterator)
      .map(_ map (j => Edge.fromJson(j.as[JsObject]).asInstanceOf[E]))
      .toIterator

  private def path(p: String) =
    client.url(baseUri + p)

  def getEgressEdges[E <: Edge](node: Node, label: Option[String], data: Map[String, JsValue]): TraversableOnce[E] =
    getEdges[E](s"/nodes/${node.id}/edges/egress", label, data)

  def getIngressEdges[E <: Edge](node: Node, label: Option[String], data: Map[String, JsValue]): TraversableOnce[E] =
    getEdges[E](s"/nodes/${node.id}/edges/ingress", label, data)

  def removeNode(node: Node): Graph = {
    path(s"/nodes/${node.id}")
      .delete()
      .map(r => if (r.status != 204) logger.warn(s"Node ${node.id} could not be found"))
      .awaitForever
    this
  }

  def removeNodes(label: Option[String], data: Map[String, JsValue]): Graph = {
    getNodes[Node](label, data)
      .map(node =>
        path(s"/nodes/${node.id}")
          .delete()
          .map(r => if (r.status != 204) logger.warn(s"Node ${node.id} could not be found"))
      )
      .foreach {
        _.awaitForever
      }
    this
  }

  def getNodes[N <: Node](label: Option[String], data: Map[String, JsValue]): TraversableOnce[N] =
    label.fold(path("/nodes"))(l => path("/nodes").withQueryString("label" -> l))
      .withBody(JsObject(data))
      .withMethod("GET")
      .stream()
      .flatMap(streamToIterator)
      .map(_ map (j => Node.fromJson(j.as[JsObject]).asInstanceOf[N]))
      .awaitForever

  def removeEdge(edge: Edge): Graph = {
    path(s"/edges/${edge.id}")
      .delete()
      .map(r => if (r.status != 204) logger.warn(s"Edge ${edge.id} could not be found"))
      .awaitForever
    this
  }

  def updateNode[N <: Node](node: N)(changes: (String, JsValue)*): N =
    path(s"/nodes/${node.id}")
      .put(JsObject(changes.toMap))
      .map(r => Node.fromJson(r.json.as[JsObject]).asInstanceOf[N])
      .awaitForever

  def updateEdge[E <: Edge](edge: E)(changes: (String, JsValue)*): E =
    path(s"/edges/${edge.id}")
      .put(JsObject(changes.toMap))
      .map(r => Edge.fromJson(r.json.as[JsObject]).asInstanceOf[E])
      .awaitForever

  def pathsTo(start: Node, end: Node, nodeLabels: Seq[String], edgeLabels: Seq[String]): TraversableOnce[Path] =
    Path.bfs(start, end, nodeLabels, edgeLabels)

}

object RemoteHttpGraph {


  private implicit class Awaiter[T](future: Future[T]) {
    def awaitForever: T =
      Await.result(future, Duration.Inf)
  }

  private implicit class FutureHelper[T](future: Future[Iterator[T]]) {
    /**
      * Produces a pseudo-lazy iterator out of this future.  It is considered pseudo-lazy because it won't block until
      * the iterator's #next() is first called.
      *
      * @return an Iterator of T
      */
    def toIterator: Iterator[T] =
      new Iterator[T] {
        private lazy val result =
          future.awaitForever

        def next(): T =
          result.next()

        def hasNext: Boolean =
          result.hasNext
      }
  }

  import akka.stream.Materializer
  import scala.concurrent.ExecutionContext
  private def streamToIterator(res: StreamedResponse)
                              (implicit materializer: Materializer,
                               executionContext: ExecutionContext): Future[Iterator[JsValue]] =
    res.body.runFold(Iterator[Char]())(_ ++ _.utf8String.toIterator)
      .map { charIterator =>
        val r = new InputStream {
          def read(): Int =
            if (charIterator.hasNext) charIterator.next() else -1
        }
        // TODO: Do actual JSON stream parsing
        Json.parse(r).as[Seq[JsValue]].toIterator
      }
}

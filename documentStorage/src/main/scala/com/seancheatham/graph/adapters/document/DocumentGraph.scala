package com.seancheatham.graph.adapters.document

import java.util.UUID

import com.seancheatham.graph.{Edge, Graph, Node, Path}
import com.seancheatham.storage.DocumentStorage
import play.api.libs.json._

import scala.concurrent.{Await, ExecutionContext, Future}

class DocumentGraph(private val db: DocumentStorage[JsValue],
                    private val basePath: String)(override implicit val nodeFactory: Node.Factory = Node.defaultFactory,
                                                  override implicit val edgeFactory: Edge.Factory = Edge.defaultFactory,
                                                  private implicit val ec: ExecutionContext = ExecutionContext.Implicits.global) extends Graph {

  private implicit class FutureHelper[T](f: Future[T]) {

    import scala.concurrent.duration._

    def await: T = Await.result(f, 10.seconds)
  }

  /**
    * A convenience for adding a node with a specific ID
    */
  def addNode[N <: Node](id: String,
                         label: String,
                         data: Map[String, JsValue]): N = {
    val node =
      nodeFactory(id, label, data)(this).asInstanceOf[N]
    db.write(basePath, "nodes", id)(Json.toJson(node)).await
    node
  }

  def addNode[N <: Node](label: String,
                         data: Map[String, JsValue]): N =
    addNode[N](UUID.randomUUID().toString, label, data)

  def addEdge[E <: Edge](id: String,
                         label: String,
                         _1: Node,
                         _2: Node,
                         data: Map[String, JsValue]): E = {
    val edge =
      edgeFactory(id, label, _1, _2, data)(this).asInstanceOf[E]
    db.write(basePath, "edges", id)(Json.toJson(edge)).await
    edge
  }

  def addEdge[E <: Edge](label: String,
                         _1: Node,
                         _2: Node,
                         data: Map[String, JsValue]): E =
    addEdge[E](UUID.randomUUID().toString, label, _1, _2, data)

  def getNode[N <: Node](id: String): Option[N] =
    db.lift(basePath, "nodes", id)
      .await
      .map(_.as[JsObject])
      .map(Node.fromJson(_).asInstanceOf[N])

  def getNodes[N <: Node](label: Option[String], data: Map[String, JsValue]): Iterator[N] = {
    val base =
      db.getCollection(basePath, "nodes")
        .await
        .map(_.as[JsObject])
    val labelFiltered =
      label.fold(base)(l => base.filter(v => (v \ "label").as[String] == l))
    val dataFiltered =
      labelFiltered
        .filter(v =>
          data
            .forall { case (key, value) => (v \ "data" \ key).toOption contains value }
        )
    dataFiltered
      .map(Node.fromJson)
      .map(_.asInstanceOf[N])
  }

  def getEdge[E <: Edge](id: String): Option[E] =
    db.lift(basePath, "edges", id)
      .await
      .map(_.as[JsObject])
      .map(Edge.fromJson(_).asInstanceOf[E])

  def getEdges[E <: Edge](label: Option[String],
                          data: Map[String, JsValue]): Iterator[E] = {
    val base =
      db.getCollection(basePath, "edges")
        .await
        .map(_.as[JsObject])
    val labelFiltered =
      label.fold(base)(l => base.filter(v => (v \ "label").as[String] == l))
    val dataFiltered =
      labelFiltered
        .filter(v =>
          data
            .forall { case (key, value) => (v \ "data" \ key).toOption contains value }
        )
    dataFiltered
      .map(Edge.fromJson)
      .map(_.asInstanceOf[E])
  }

  def getEgressEdges[E <: Edge](node: Node, edgeLabel: Option[String], edgeData: Map[String, JsValue]): Iterator[E] = {
    val base =
      db.getCollection(basePath, "edges")
        .await
        .map(_.as[JsObject])
    val nodeFiltered =
      base.filter(v => (v \ "_1").as[String] == node.id)
    val labelFiltered =
      edgeLabel.fold(nodeFiltered)(l => nodeFiltered.filter(v => (v \ "label").as[String] == l))
    val dataFiltered =
      labelFiltered
        .filter(v =>
          edgeData
            .forall { case (key, value) => (v \ "data" \ key).toOption contains value }
        )
    dataFiltered
      .map(Edge.fromJson)
      .map(_.asInstanceOf[E])
  }


  def getIngressEdges[E <: Edge](node: Node, edgeLabel: Option[String], edgeData: Map[String, JsValue]): Iterator[E] = {
    val base =
      db.getCollection(basePath, "edges")
        .await
        .map(_.as[JsObject])
    val nodeFiltered =
      base.filter(v => (v \ "_2").as[String] == node.id)
    val labelFiltered =
      edgeLabel.fold(nodeFiltered)(l => nodeFiltered.filter(v => (v \ "label").as[String] == l))
    val dataFiltered =
      labelFiltered
        .filter(v =>
          edgeData
            .forall { case (key, value) => (v \ "data" \ key).toOption contains value }
        )
    dataFiltered
      .map(Edge.fromJson)
      .map(_.asInstanceOf[E])
  }

  def removeNode(node: Node): DocumentGraph = {
    (getIngressEdges(node, None, Map.empty) ++
      getEgressEdges(node, None, Map.empty))
      .foreach(removeEdge)
    db.delete(basePath, "nodes", node.id).await
    this
  }

  def removeNodes(label: Option[String], data: Map[String, JsValue]): DocumentGraph = {
    getNodes(label, data).foreach(removeNode)
    this
  }

  def removeEdge(edge: Edge): DocumentGraph = {
    db.delete(basePath, "edges", edge.id).await
    this
  }

  def updateNode[N <: Node](node: N)(changes: (String, JsValue)*): N =
    db.merge(basePath, "nodes", node.id, "data")(JsObject(changes.toMap))
      .map(_ => getNode[N](node.id).get)
      .await

  def updateEdge[E <: Edge](edge: E)(changes: (String, JsValue)*): E =
    db.merge(basePath, "edges", edge.id, "data")(JsObject(changes.toMap))
      .map(_ => getEdge[E](edge.id).get)
      .await

  /**
    * Performs Breadth-First Search, returning a collection with the single item
    * being a path from start to end.
    */
  def pathsTo(start: Node,
              end: Node,
              nodeLabels: Seq[String],
              edgeLabels: Seq[String]): Vector[Path] =
    Path.bfs(start, end, nodeLabels, edgeLabels)
}
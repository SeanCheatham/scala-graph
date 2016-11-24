package com.seancheatham.graph.adapters.memory

import java.util.UUID

import com.seancheatham.graph.{Edge, Graph, Node, Path}
import play.api.libs.json.JsValue

import scala.collection.mutable

class MutableGraph(override implicit val nodeFactory: Node.Factory = Node.defaultFactory,
                   override implicit val edgeFactory: Edge.Factory = Edge.defaultFactory) extends Graph {

  private val nodes =
    mutable.Map.empty[String, Node]

  private val edges =
    mutable.Map.empty[String, Edge]

  def toImmutableGraph =
    ImmutableGraph(
      nodes.toMap.mapValues(_.toConstruct),
      edges.toMap.mapValues(_.toConstruct)
    )

  def addNode[N <: Node](label: String,
                         data: Map[String, JsValue]) = {
    val id =
      UUID.randomUUID().toString
    val node =
      nodeFactory(id, label, data)(this).asInstanceOf[N]
    nodes += (id -> node)
    node
  }

  def addEdge[E <: Edge](label: String,
                         _1: Node,
                         _2: Node,
                         data: Map[String, JsValue]) = {
    val id =
      UUID.randomUUID().toString
    val edge =
      edgeFactory(id, label, _1, _2, data)(this).asInstanceOf[E]
    edges += (id -> edge)
    edge
  }

  def getNode[N <: Node](id: String) =
    nodes get id map (_.asInstanceOf[N])

  def getNodes[N <: Node](label: Option[String], data: Map[String, JsValue]) =
    label
      .fold(nodes.values)(l =>
        nodes.values.filter(_.label == l)
      )
      .toIterator
      .filter(n =>
        data
          .forall { case (key, value) => n.data(key) == value }
      )
      .map(_.asInstanceOf[N])

  def getEdge[E <: Edge](id: String) =
    edges get id map (_.asInstanceOf[E])

  def getEdges[E <: Edge](label: Option[String],
                          data: Map[String, JsValue]) =
    edges.valuesIterator
      .collect {
        case e: E@unchecked if label.fold(true)(_ == e.label) &&
          data
            .forall { case (key, value) => e.data(key) == value } =>
          e
      }

  def getEgressEdges[E <: Edge](node: Node, edgeLabel: Option[String], edgeData: Map[String, JsValue]) =
    edgeLabel
      .fold(edges.values)(l =>
        edges.values.filter(_.label == l)
      )
      .toIterator
      .filter(e =>
        edgeData
          .forall { case (key, value) => e.data(key) == value }
      )
      .filter(_._1 == node)
      .map(_.asInstanceOf[E])


  def getIngressEdges[E <: Edge](node: Node, edgeLabel: Option[String], edgeData: Map[String, JsValue]) =
    edgeLabel
      .fold(edges.values)(l =>
        edges.values.filter(_.label == l)
      )
      .toIterator
      .filter(e =>
        edgeData
          .forall { case (key, value) => e.data(key) == value }
      )
      .filter(_._2 == node)
      .map(_.asInstanceOf[E])

  def removeNode(node: Node) = {
    nodes -= node.id
    this
  }

  def removeNodes(label: Option[String], data: Map[String, JsValue]) = {
    nodes --= nodes.collect {
      case (id, n) if label.fold(true)(_ == n.label) &&
        data.forall { case (key, value) => n.data(key) == value } =>
        id
    }
    this
  }

  def removeEdge(edge: Edge) = {
    edges -= edge.id
    this
  }

  def updateNode[N <: Node](node: N)(changes: (String, JsValue)*) = {
    val oldConstruct =
      node.toConstruct
    val newConstruct =
      (oldConstruct._1, oldConstruct._2, oldConstruct._3 ++ changes)
    val newNode =
      nodeFactory(newConstruct)(this).asInstanceOf[N]
    nodes.update(node.id, newNode)
    newNode
  }

  def updateEdge[E <: Edge](edge: E)(changes: (String, JsValue)*) = {
    val oldConstruct =
      edge.toConstruct
    val newConstruct =
      (oldConstruct._1, oldConstruct._2, oldConstruct._3, oldConstruct._4, oldConstruct._5 ++ changes)
    val newEdge =
      edgeFactory(newConstruct)(this).asInstanceOf[E]
    edges.update(edge.id, newEdge)
    newEdge
  }

  /**
    * Performs Breadth-First Search, returning a collection with the single item
    * being a path from start to end.
    */
  def pathsTo(start: Node,
              end: Node,
              nodeLabels: Seq[String],
              edgeLabels: Seq[String]) = {
    val distances =
      mutable.Map.empty[Node, Int]

    val parents =
      mutable.Map.empty[Node, (Edge, Boolean)]

    val queue =
      mutable.Queue[Node](start)

    var continue =
      true

    while (continue && queue.nonEmpty) {

      val current =
        queue.dequeue()

      def f(items: Iterator[Edge],
            direction: Boolean) = {

        val cleanItems =
          items
            .filter(edgeLabels contains _.label)
            .filter(nodeLabels contains _._1.label)
            .filter(nodeLabels contains _._2.label)

        while (continue && cleanItems.hasNext) {
          val edge =
            cleanItems.next()

          val node =
            if (direction)
              edge._2
            else
              edge._1

          if (!distances.contains(node)) {
            distances += (node -> (distances.getOrElse(current, 0) + 1))
            parents += (node -> (edge, direction))
            queue.enqueue(node)
          }

          if (node == end)
            continue = false
        }
      }

      f(current.egressEdges[Edge]().toIterator, direction = true)
      f(current.ingressEdges[Edge]().toIterator, direction = false)
    }

    val trail =
      mutable.Stack[Edge]()

    var node =
      end

    while (node != start) {
      val (edge, direction) =
        parents(node)

      trail.push(edge)

      if (direction)
        node = edge._1
      else
        node = edge._2
    }

    val path =
      Path(start, trail.toVector)

    Vector(path)
  }
}

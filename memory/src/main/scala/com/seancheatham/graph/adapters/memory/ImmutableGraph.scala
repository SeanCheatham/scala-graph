package com.seancheatham.graph.adapters.memory

import com.seancheatham.graph.{Edge, Graph, Node, Path}
import play.api.libs.json.JsValue

case class ImmutableGraph(private val nodeConstructs: Map[String, Node.Construct] = Map.empty,
                          private val edgeConstructs: Map[String, Edge.Construct] = Map.empty)
                         (override implicit val nodeFactory: Node.Factory = Node.defaultFactory,
                          override implicit val edgeFactory: Edge.Factory = Edge.defaultFactory) extends Graph {

  private def nextNodeId =
    nodeConstructs.size.toString

  private def nextEdgeId =
    edgeConstructs.size.toString

  def toMutableGraph = {
    val newGraph =
      new MutableGraph

    val nodeIdMapping =
      nodeConstructs
        .mapValues(nc => newGraph.addNode[Node](nc._2, nc._3).id)

    val newEdges =
      edgeConstructs
        .valuesIterator
        .map(e =>
          (e._1,
            e._2,
            newGraph.getNode[Node](nodeIdMapping(e._3.id)).get,
            newGraph.getNode[Node](nodeIdMapping(e._4.id)).get,
            e._5)
        )
    newEdges.foreach {
      e =>
        newGraph.addEdge(e._2, e._3, e._4, e._5)
    }

    newGraph
  }

  def addNode[N <: Node](label: String,
                         data: Map[String, JsValue]) = {
    val id =
      nextNodeId
    val construct =
      (id, label, data)
    val g1 =
      this.copy(
        nodeConstructs = nodeConstructs + (id -> construct)
      )(nodeFactory, edgeFactory)
    nodeFactory(construct)(g1).asInstanceOf[N]
  }

  def addEdge[E <: Edge](label: String,
                         _1: Node,
                         _2: Node,
                         data: Map[String, JsValue]) = {
    val id =
      nextEdgeId
    val construct =
      (id, label, getNode[Node](_1.id).get, getNode[Node](_2.id).get, data)
    val g1 =
      this.copy(
        edgeConstructs = edgeConstructs + (id -> construct)
      )(nodeFactory, edgeFactory)
    edgeFactory(construct)(g1).asInstanceOf[E]
  }

  def getNode[N <: Node](id: String) =
    nodeConstructs get id map (nodeFactory(_)(this).asInstanceOf[N])

  def getNodes[N <: Node](label: Option[String], data: Map[String, JsValue]) =
    label
      .fold(nodeConstructs.values)(l =>
        nodeConstructs.values.filter(_._2 == l)
      )
      .toIterator
      .filter(n =>
        data
          .forall { case (key, value) => n._3(key) == value }
      )
      .map(nodeFactory(_)(this).asInstanceOf[N])

  def getEgressEdges[E <: Edge](node: Node, edgeLabel: Option[String], edgeData: Map[String, JsValue]) =
    edgeLabel
      .fold(edgeConstructs.values)(l =>
        edgeConstructs.values.filter(_._2 == l)
      )
      .toIterator
      .filter(e =>
        edgeData
          .forall { case (key, value) => e._5(key) == value }
      )
      .filter(_._3.id == node.id)
      .map(edgeFactory(_)(this))
      .map(_.asInstanceOf[E])


  def getIngressEdges[E <: Edge](node: Node, edgeLabel: Option[String], edgeData: Map[String, JsValue]) =
    edgeLabel
      .fold(edgeConstructs.values)(l =>
        edgeConstructs.values.filter(_._2 == l)
      )
      .toIterator
      .filter(e =>
        edgeData
          .forall { case (key, value) => e._5(key) == value }
      )
      .filter(_._4.id == node.id)
      .map(edgeFactory(_)(this))
      .map(_.asInstanceOf[E])

  def removeNode(node: Node) =
    this.copy(
      nodeConstructs = nodeConstructs - node.id
    )(nodeFactory, edgeFactory)

  def removeNodes(label: Option[String], data: Map[String, JsValue]) = {
    val newNodeConstructs = {
      val withoutLabel =
        label.fold(nodeConstructs)(l => nodeConstructs.filterNot(_._2._2 == l))
      withoutLabel
        .filterNot(kv => data.forall { case (key, value) => kv._2._3(key) == value })
    }
    val newEdges =
      edgeConstructs filterNot (kv =>
        !newNodeConstructs.contains(kv._2._3.id) ||
          !newNodeConstructs.contains(kv._2._4.id)
        )
    ImmutableGraph(newNodeConstructs, newEdges)(nodeFactory, edgeFactory)
  }

  def removeEdge(edge: Edge) =
    this.copy(
      edgeConstructs = edgeConstructs - edge.id
    )(nodeFactory, edgeFactory)

  def updateNode[N <: Node](node: N)(changes: (String, JsValue)*) = {
    val oldConstruct =
      node.toConstruct
    val newConstruct =
      (oldConstruct._1, oldConstruct._2, oldConstruct._3 ++ changes)
    val newGraph =
      this.copy(
        nodeConstructs.updated(node.id, newConstruct)
      )(nodeFactory, edgeFactory)
    nodeFactory(newConstruct)(newGraph).asInstanceOf[N]
  }

  def updateEdge[E <: Edge](edge: E)(changes: (String, JsValue)*) = {
    val oldConstruct =
      edge.toConstruct
    val newConstruct =
      (oldConstruct._1, oldConstruct._2, getNode[Node](oldConstruct._3.id).get, getNode[Node](oldConstruct._4.id).get, oldConstruct._5 ++ changes)
    val newGraph =
      this.copy(
        edgeConstructs = edgeConstructs.updated(edge.id, newConstruct)
      )(nodeFactory, edgeFactory)
    edgeFactory(newConstruct)(newGraph).asInstanceOf[E]
  }

  /**
    * Performs Breadth-First Search, returning a collection with the single item
    * being a path from start to end.
    */
  def pathsTo(start: Node,
              end: Node,
              nodeLabels: Seq[String],
              edgeLabels: Seq[String]) = {

    import scala.collection.mutable

    val distances =
      mutable.Map.empty[String, Int]

    val parents =
      mutable.Map.empty[String, (Edge, Boolean)]

    val queue =
      mutable.Queue[String](start.id)

    var continue =
      true

    while (continue && queue.nonEmpty) {

      val current =
        getNode[Node](
          queue.dequeue()
        ).get

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

          if (!distances.contains(node.id)) {
            distances += (node.id -> (distances.getOrElse(current.id, 0) + 1))
            parents += (node.id -> (edge, direction))
            queue.enqueue(node.id)
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
        parents(node.id)

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

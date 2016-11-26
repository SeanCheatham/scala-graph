package com.seancheatham.graph

import play.api.libs.json.{JsValue, Json, Writes}

/**
  * A "Graph" is an interconnected web of data.  Containers of data ([[com.seancheatham.graph.Node]])
  * form relationships ([[com.seancheatham.graph.Edge]]) with other containers.  This type of data
  * structure is capable of representing many types of higher level concepts, such as
  * dependency management, social networking, syntax trees, etc.
  */
abstract class Graph {

  /**
    * Implicit self-reference
    */
  implicit def graph: Graph =
    this

  /**
    * A method which constructs a Node using the given information
    *
    * @return a Node
    */
  implicit def nodeFactory: Node.Factory =
    Node.defaultFactory

  /**
    * A method which constructs an Edge using the given information
    *
    * @return an Edge
    */
  implicit def edgeFactory: Edge.Factory =
    Edge.defaultFactory

  def addNode[N <: Node](label: String,
                         data: Map[String, JsValue]): N

  def addEdge[E <: Edge](label: String,
                         _1: Node,
                         _2: Node,
                         data: Map[String, JsValue]): E

  /**
    * A helper alias for addEdge, which supports using the following syntax:
    *
    * .addEdge(a -"KNOWS"-> b)
    *
    * @see [[com.seancheatham.graph.Edge.NodeEdgeSyntax]]
    * @param e    A tuple in the form of ((_1, label), _2)
    * @param data The edge's meta-data
    * @return a Graph with this edge added
    */
  def addEdge[E <: Edge](e: ((Node, String), Node),
                         data: Map[String, JsValue] = Map.empty): E =
    addEdge[E](e._1._2, e._1._1, e._2, data)

  def getNode[N <: Node](id: String): Option[N]

  def getNodes[N <: Node](label: Option[String] = None,
                          data: Map[String, JsValue] = Map.empty): TraversableOnce[N]

  def getEdge[E <: Edge](id: String): Option[E]

  def getEdges[E <: Edge](label: Option[String] = None,
                          data: Map[String, JsValue] = Map.empty): TraversableOnce[E]

  def getEgressEdges[E <: Edge](node: Node,
                                edgeLabel: Option[String] = None,
                                edgeData: Map[String, JsValue] = Map.empty): TraversableOnce[E]

  def getIngressEdges[E <: Edge](node: Node,
                                 edgeLabel: Option[String] = None,
                                 edgeData: Map[String, JsValue] = Map.empty): TraversableOnce[E]

  def removeNode(node: Node): Graph

  def removeNodes(label: Option[String] = None,
                  data: Map[String, JsValue] = Map.empty): Graph

  def removeEdge(edge: Edge): Graph

  def updateNode[N <: Node](node: N)(changes: (String, JsValue)*): N

  def updateEdge[E <: Edge](edge: E)(changes: (String, JsValue)*): E

  def pathsTo(start: Node,
              end: Node,
              nodeLabels: Seq[String] = Seq.empty,
              edgeLabels: Seq[String] = Seq.empty): TraversableOnce[Path]

  /**
    * Insert all of the nodes and edges from the other graph into this one, returning a new combined graph
    *
    * Since ID schemes vary depending on the storage system, all IDs have to be re-generated.  The IDs will
    * be mapped over, however doing so is handled in-memory.  As a result, excessively large
    * graphs (with long ID keys) may max out memory on lighter systems.
    *
    * @param other The other graph to merge into this one
    * @return a new Graph, with all of the nodes and edges from the other graph merged in
    */
  def ++(other: Graph): Graph = {
    val (g1, nodeIdMapping) =
      other.getNodes[Node]()
        .foldLeft((this, Map.empty[String, String])) {
          case ((g2, mapping), node) =>
            val newNode =
              g2.addNode[Node](node.label, node.data)
            (newNode.graph, mapping + (node.id -> newNode.id))
        }
    other.getEdges[Edge]()
      .foldLeft(g1) {
        case (g2, edge) =>
          val _1 =
            g2.getNode[Node](nodeIdMapping(edge._1.id)).get
          val _2 =
            g2.getNode[Node](nodeIdMapping(edge._2.id)).get
          g2.addEdge[Edge](edge.label, _1, _2, edge.data).graph
      }
  }

}

object Graph {

  implicit val writes: Writes[Graph] =
    Writes[Graph](
      graph =>
        Json.obj(
          "nodes" -> graph.getNodes[Node]().toSeq,
          "edges" -> graph.getEdges[Edge]().toSeq
        )
    )

}
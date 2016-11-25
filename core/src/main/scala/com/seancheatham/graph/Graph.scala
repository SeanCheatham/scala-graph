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
    *
    * @param e A tuple in the form of ((_1, label), _2)
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
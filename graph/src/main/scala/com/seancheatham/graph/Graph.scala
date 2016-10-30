package com.seancheatham.graph

import play.api.libs.json.JsValue

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
  implicit def graph =
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

  /**
    * Inserts a node into this graph
    *
    * @param label The node's label
    * @param data The node's meta-data
    * @return a Graph with this node added
    */
  def addNode[N <: Node](label: String,
                         data: Map[String, JsValue]): N

  /**
    * Inserts an edge between two nodes in this graph
    *
    * @param label The edge's label
    * @param data The edge's meta-data
    * @return a Graph with this edge added
    */
  def addEdge[E <: Edge](_1: Node,
                         _2: Node,
                         label: String,
                         data: Map[String, JsValue]): E

  /**
    * Retrieves a node by ID from this graph
    *
    * @param id The node's ID
    * @return an optional Node
    */
  def getNode[N <: Node](id: String): Option[N]

  /**
    * Retrieves all nodes which match the given label and items in given data mapping
    *
    * @param label The optional label which all returned nodes must have
    * @param data A key-value pairing which much match the returned nodes
    * @return a collection of nodes
    */
  def getNodes[N <: Node](label: Option[String],
                          data: Map[String, JsValue]): TraversableOnce[N]

  /**
    * Retrieves all outgoing edges from the given node
    *
    * @param node The _1 node
    * @param edgeLabel An optional node label to match
    * @param edgeData A key-value pairing which must match the returned edges
    * @return a collection of edges
    */
  def getEgressEdges[E <: Edge](node: Node,
                                edgeLabel: Option[String],
                                edgeData: Map[String, JsValue]): TraversableOnce[E]

  /**
    * Retrieves all incoming edges to the given node
    *
    * @param node The _2 node
    * @param edgeLabel An optional node label to match
    * @param edgeData A key-value pairing which must match the returned edges
    * @return a collection of edges
    */
  def getIngressEdges[E <: Edge](node: Node,
                                 edgeLabel: Option[String],
                                 edgeData: Map[String, JsValue]): TraversableOnce[E]

  /**
    * Removes the given Node from this Graph
    *
    * @param node The node to remove
    * @return a Graph with the node deleted
    */
  def removeNode(node: Node): Graph

  /**
    * Removes all nodes matching the given optional label and data
    *
    * @param label an optional label to filter
    * @param data a key-value pairing to match
    * @return a Graph with the node deleted
    */
  def removeNodes(label: Option[String],
                  data: Map[String, JsValue]): Graph

  /**
    * Removes the given Edge from this Graph
    *
    * @param edge The edge to remove
    * @return a Graph with the edge deleted
    */
  def removeEdge(edge: Edge): Graph

  /**
    * Updates the given Node, changing its data key-values using the given parameters
    *
    * @param node The Node to update
    * @param changes The changes to make
    * @return an updated Node
    */
  def updateNode[N <: Node](node: N)(changes: (String, JsValue)*): N

  /**
    * Updates the given Edge, changing its data key-values using the given parameters
    *
    * @param edge The Edge to update
    * @param changes The changes to make
    * @return an updated Edge
    */
  def updateEdge[E <: Edge](edge: E)(changes: (String, JsValue)*): E

}

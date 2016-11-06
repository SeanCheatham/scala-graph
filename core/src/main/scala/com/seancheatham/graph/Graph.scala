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
  def getNodes[N <: Node](label: Option[String] = None,
                          data: Map[String, JsValue] = Map.empty): TraversableOnce[N]

  /**
    * Fetch the edge with the given ID
    *
    * @param id The edge's ID
    * @tparam E Edge type
    * @return An optional Edge if found
    */
  def getEdge[E <: Edge](id: String): Option[E]

  /**
    * Retrieves all edge which match the given label and items in given data mapping
    *
    * @param label The optional label which all returned edges must have
    * @param data A key-value pairing which much match the returned edges
    * @return a collection of edges
    */
  def getEdges[E <: Edge](label: Option[String] = None,
                          data: Map[String, JsValue] = Map.empty): TraversableOnce[E]

  /**
    * Retrieves all outgoing edges from the given node
    *
    * @param node The _1 node
    * @param edgeLabel An optional node label to match
    * @param edgeData A key-value pairing which must match the returned edges
    * @return a collection of edges
    */
  def getEgressEdges[E <: Edge](node: Node,
                                edgeLabel: Option[String] = None,
                                edgeData: Map[String, JsValue] = Map.empty): TraversableOnce[E]

  /**
    * Retrieves all incoming edges to the given node
    *
    * @param node The _2 node
    * @param edgeLabel An optional node label to match
    * @param edgeData A key-value pairing which must match the returned edges
    * @return a collection of edges
    */
  def getIngressEdges[E <: Edge](node: Node,
                                 edgeLabel: Option[String] = None,
                                 edgeData: Map[String, JsValue] = Map.empty): TraversableOnce[E]

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
  def removeNodes(label: Option[String] = None,
                  data: Map[String, JsValue] = Map.empty): Graph

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

  /**
    * Finds all of the paths between the given node and the destination.
    * 
    * Optional filters can be provided and applied to items in the search.
    * 
    * @param start The starting Node
    * @param end The target destination Node
    * @param nodeLabels An inclusive list of labels to accept for nodes in the path
    *                    An empty list will accept all nodes
    * @param edgeLabels An inclusive list of labels to accept for edges in the path
    *                    An empty list will accept all edges
    * @return a collection of Paths, where the order is up to an implementer
    */
  def pathsTo(start: Node,
              end: Node,
              nodeLabels: Seq[String] = Seq.empty,
              edgeLabels: Seq[String] = Seq.empty): TraversableOnce[Path]

}

object Graph {

  implicit val writes =
    Writes[Graph](
      graph =>
        Json.obj(
          "nodes" ->
            graph.getNodes[Node]()
              .toSeq
              .map(node =>
                Json.obj(
                  "id" -> node.id,
                  "label" -> node.label,
                  "data" -> node.data
                )
              ),
          "edges" ->
            graph.getEdges[Edge]()
              .toSeq
              .map(edge =>
                Json.obj(
                  "id" -> edge.id,
                  "label" -> edge.label,
                  "_1" -> edge._1.id,
                  "_2" -> edge._2.id,
                  "data" -> edge.data
                )
              )
        )
    )

}
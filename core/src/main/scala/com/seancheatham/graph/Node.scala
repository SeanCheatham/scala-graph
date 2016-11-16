package com.seancheatham.graph

import play.api.libs.json.{JsObject, JsValue}

/**
  * A node represents a container of data.  Nodes can be classified or categorized by some label.
  * Since nodes are data containers, they have JSON-serializable data.  Nodes can also be
  * connected to other nodes in the form of [[com.seancheatham.graph.Edge]]s.
  */
abstract class Node {
  import com.seancheatham.graph.utility.JsonTools.JsonMapHelper

  /**
    * This node's hosting graph
    */
  implicit def graph: Graph

  /**
    * The unique identifier of this node, within the context of its graph
    */
  def id: String

  /**
    * The label/type/name of this node
    */
  def label: String

  /**
    * Meta-data properties of this node
    */
  def data: Map[String, JsValue]

  /**
    * Exports this Node as a [[com.seancheatham.graph.Node#Construct]]
    * @return a Node.NodeConstruct
    */
  def toConstruct: Node.Construct =
    (id, label, data.withoutNulls)

  /**
    * Fetches all incoming edges to this Node, with optional matches on label and data
    *
    * @param edgeLabel An optional edge label filter
    * @param edgeData Key-value pairs which must be fulfilled
    * @return an Iterator of edges
    */
  def ingressEdges[E <: Edge](edgeLabel: Option[String] = None,
                              edgeData: Map[String, JsValue] = Map.empty) =
    graph.getIngressEdges[E](this, edgeLabel, edgeData)

  /**
    * Fetches all outgoing edges from this Node, with optional matches on label and data
    *
    * @param edgeLabel An optional edge label filter
    * @param edgeData Key-value pairs which must be fulfilled
    * @return an Iterator of edges
    */
  def egressEdges[E <: Edge](edgeLabel: Option[String] = None,
                             edgeData: Map[String, JsValue] = Map.empty) =
    graph.getEgressEdges[E](this, edgeLabel, edgeData)

  /**
    * Creates an instance of this Node in the graph.  This is a convenience which allows for node
    * sub-classes. and then inserting them.
    *
    * @return A NEW node (with a different ID), with a potentially different node.graph
    */
  def create[N <: Node] =
    graph.addNode[N](this.label, this.data.withoutNulls)

  /**
    * Updates this node in the graph by overwriting all data values.
    *
    * @return A NEW, updated node, with a potentially different node.graph
    */
  def update[N <: Node] =
    graph.updateNode[N](this.asInstanceOf[N])(this.data.toSeq: _*)

  /**
    * Creates an edge with the given label, to the given node, with the given data.  Inserts the edge into this.graph.
    *
    * @param label The edge's label
    * @param _2 The destination node
    * @param data The edge's data
    * @tparam E some type of Edge
    * @return A NEW edge.  The edge's graph may be different from this node's graph, so throw this node away.
    */
  def createEdgeTo[E <: Edge](label: String,
                              _2: Node,
                              data: Map[String, JsValue] = Map.empty) =
    graph.addEdge[E](label, this, _2, data)

  /**
    * Removes this node from the graph
    *
    * @return A new graph with this node deleted
    */
  def remove =
    graph.removeNode(this)

}

object Node {

  type Construct = (String, String, Map[String, JsValue])

  type Factory = Construct => Graph => Node

  implicit final def defaultFactory(construct: Construct)(nGraph: Graph) =
    DefaultNode(construct._1, construct._2, construct._3)(nGraph)

  def fromJson(json: JsObject)(implicit graph: Graph): Node =
    graph.nodeFactory(
      (
        (json \ "id").as[String],
        (json \ "label").as[String],
        (json \ "data").as[Map[String, JsValue]]
        )
    )(graph)

}

case class DefaultNode(id: String,
                       label: String,
                       data: Map[String, JsValue])(implicit val graph: Graph) extends Node

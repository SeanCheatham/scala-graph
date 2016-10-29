package com.seancheatham.graph

import play.api.libs.json.{JsObject, JsValue}

/**
  * A node represents a container of data.  Nodes can be classified or categorized by some label.
  * Since nodes are data containers, they have JSON-serializable data.  Nodes can also be
  * connected to other nodes in the form of [[com.seancheatham.graph.Edge]]s.
  */
abstract class Node {

  /**
    * This node's hosting graph
    */
  def graph: Graph

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
    (id, label, data)

}

object Node {

  type Construct = (String, String, Map[String, JsValue])

  type Factory = Construct => Graph => Node

  def fromJson(json: JsObject)(implicit graph: Graph): Node =
    graph.nodeFactory(
      (
        (json \ "id").as[String],
        (json \ "label").as[String],
        (json \ "data").as[Map[String, JsValue]]
        )
    )(graph)

}

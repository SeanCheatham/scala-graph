package com.seancheatham.graph

import play.api.libs.json.{JsObject, JsValue}

/**
  * The core representation of an "Edge" between two [[com.seancheatham.graph.Node]]s.  If a Node is a
  * container of data then an Edge represents a relationship between two containers.  An edge itself
  * has a label, as well as extra meta-data represented as a JSON-serializable mapping.
  */
abstract class Edge {

  /**
    * This edge's hosting graph
    */
  implicit def graph: Graph

  /**
    * The unique identifier of this edge, within the context of its graph
    */
  def id: String

  /**
    * The label/type/name of this edge
    */
  def label: String

  /**
    * Extra meta-data properties of this edge
    */
  def data: Map[String, JsValue]

  /**
    * The "head"/"a"/"start" node
    */
  def _1: Node

  /**
    * The "last"/"b"/"end" node
    * @return
    */
  def _2: Node

  /**
    * Creates an instance of this Edge in the graph.  This is a convenience which allows for edge
    * sub-classes. and then inserting them.
    *
    * @return A NEW edge (with a different ID), with a potentially different edge.graph
    */
  def create[E <: Edge] =
    graph.addEdge[E](this.label, _1, _2, this.data)

  /**
    * Updates this edge in the graph by overwriting all data values.
    *
    * @return A NEW, updated edge, with a potentially different edge.graph
    */
  def update[E <: Edge] =
    graph.updateEdge[E](this.asInstanceOf[E])(this.data.toSeq: _*)

  /**
    * Removes this edge from the graph
    *
    * @return A new graph with this edge deleted
    */
  def remove =
    graph.removeEdge(this)

  /**
    * Exports this Edge as a [[com.seancheatham.graph.Edge#Construct]]
    * @return an Edge.EdgeConstruct
    */
  def toConstruct: Edge.Construct =
    (id, label, _1, _2, data)

}

object Edge {

  // ID, Label, _1, _2, Data
  type Construct = (String, String, Node, Node, Map[String, JsValue])

  type Factory = Construct => Graph => Edge

  implicit final def defaultFactory(construct: Construct)(nGraph: Graph) =
    DefaultEdge(
      construct._1,
      construct._2,
      construct._3,
      construct._4,
      construct._5
    )(nGraph)

  def fromJson(json: JsObject,
               _1: Node,
               _2: Node)(implicit graph: Graph): Edge =
    graph.edgeFactory(
      (
        (json \ "id").as[String],
        (json \ "label").as[String],
        _1,
        _2,
        (json \ "data").as[Map[String, JsValue]]
        )
    )(graph)

  @throws(classOf[java.util.NoSuchElementException])
  def fromJson[N1 <: Node, N2 <: Node](json: JsObject,
                       getNode1: String => Option[N1],
                       getNode2: String => Option[N2])(implicit graph: Graph): Edge =
    graph.edgeFactory(
      (
        (json \ "id").as[String],
        (json \ "label").as[String],
        getNode1((json \ "_1").as[String]).get,
        getNode1((json \ "_2").as[String]).get,
        (json \ "data").as[Map[String, JsValue]]
        )
    )(graph)

  implicit class NodeEdgeSyntax(node: Node) {
    def -(label: String) =
      node -> label
  }

}

case class DefaultEdge(id: String,
                       label: String,
                       _1: Node,
                       _2: Node,
                       data: Map[String, JsValue])(implicit val graph: Graph) extends Edge

case class DefaultWeightedEdge(id: String,
                               label: String,
                               _1: Node,
                               _2: Node,
                               data: Map[String, JsValue],
                               weight: Float)(implicit val graph: Graph) extends Edge

object DefaultWeightedEdge {
  def apply(id: String,
            label: String,
           _1: Node,
           _2: Node,
            data: Map[String, JsValue])(implicit graph: Graph): DefaultWeightedEdge =
    DefaultWeightedEdge(
      id,
      label,
      _1,
      _2,
      data - "weight",
      data("weight").as[Float]
    )(graph)
}
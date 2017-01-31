package com.seancheatham.graph

import play.api.libs.json._

/**
  * The core representation of an "Edge" between two [[com.seancheatham.graph.Node]]s.  If a Node is a
  * container of data then an Edge represents a relationship between two containers.  An edge itself
  * has a label, as well as extra meta-data represented as a JSON-serializable mapping.
  */
abstract class Edge {

  import com.seancheatham.graph.utility.JsonTools.JsonMapHelper

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
    *
    * @return
    */
  def _2: Node

  /**
    * Creates an instance of this Edge in the graph.  This is a convenience which allows for edge
    * sub-classes. and then inserting them.
    *
    * @return A NEW edge (with a different ID), with a potentially different edge.graph
    */
  def create[E <: Edge]: E =
    graph.addEdge[E](this.label, _1, _2, this.data.withoutNulls)

  /**
    * Updates this edge in the graph by overwriting all data values.
    *
    * @return A NEW, updated edge, with a potentially different edge.graph
    */
  def update[E <: Edge]: E =
    graph.updateEdge[E](this.asInstanceOf[E])(this.data.toSeq: _*)

  /**
    * Removes this edge from the graph
    *
    * @return A new graph with this edge deleted
    */
  def remove: Graph =
    graph.removeEdge(this)

  /**
    * Exports this Edge as a [[com.seancheatham.graph.Edge#Construct]]
    *
    * @return an Edge.EdgeConstruct
    */
  def toConstruct: Edge.Construct =
    (id, label, _1, _2, data.withoutNulls)

}

object Edge {

  // ID, Label, _1, _2, Data
  type Construct = (String, String, Node, Node, Map[String, JsValue])

  type Factory = Construct => Graph => Edge

  /**
    * The default/base factory for edges.  Will construct a [[DefaultLazyEdge]].
    *
    * @param construct The edge construct to apply
    * @param nGraph    The edge's parent graph
    * @return An edge
    */
  implicit final def defaultFactory(construct: Construct)(nGraph: Graph): Edge =
    new DefaultLazyEdge(
      construct._1,
      construct._2,
      construct._3,
      construct._4,
      construct._5
    )(nGraph)

  def fromJson(json: JsObject)(implicit graph: Graph): Edge =
    fromJson(json, graph.getNode[Node] _, graph.getNode[Node] _)

  def fromJson(json: JsObject,
               _1: => Node,
               _2: => Node)(implicit graph: Graph): Edge =
    graph.edgeFactory(
      (
        (json \ "id").as[String],
        (json \ "label").as[String],
        _1,
        _2,
        (json \ "data").asOpt[Map[String, JsValue]].getOrElse(Map.empty)
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
        (json \ "data").asOpt[Map[String, JsValue]].getOrElse(Map.empty)
      )
    )(graph)

  implicit class NodeEdgeSyntax(node: Node) {
    def -(label: String): (Node, String) =
      node -> label
  }

  implicit val writesConstruct: Writes[Construct] =
    Writes[Construct](
      construct =>
        Json.obj(
          "id" -> construct._1,
          "label" -> construct._2,
          "_1" -> construct._3.id,
          "_2" -> construct._4.id,
          "data" -> construct._5
        )
    )

  implicit val writes: Writes[Edge] =
    Writes[Edge](edge => Json.toJson(edge.toConstruct))

  implicit def reads(implicit graph: Graph): Reads[Construct] =
    Reads[Construct](
      json =>
        JsSuccess(
          (
            (json \ "id").as[String],
            (json \ "label").as[String],
            graph.getNode[Node]((json \ "_1").as[String]).get,
            graph.getNode[Node]((json \ "_2").as[String]).get,
            (json \ "data").asOpt[Map[String, JsValue]].getOrElse(Map.empty)
          )
        )
    )

}

/**
  * An edge whose _1 and _2 are lazy evaluated
  *
  * @param id    The Edge's ID
  * @param label The Edge's Label
  * @param __1   The Edge's (lazy) _1
  * @param __2   The Edge's (lazy) _2
  * @param data  The Edge's Data
  * @param graph The Edge's parent graph
  */
class DefaultLazyEdge(val id: String,
                      val label: String,
                      __1: => Node,
                      __2: => Node,
                      val data: Map[String, JsValue])(implicit val graph: Graph) extends Edge {
  def _1: Node = __1

  def _2: Node = __2
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
package com.seancheatham.graph.adapters.memory

import java.util.UUID

import com.seancheatham.graph.{Edge, Graph, Node}
import play.api.libs.json.JsValue

import scala.collection.mutable

class MutableGraph extends Graph {

  private val nodes =
    mutable.Map.empty[String, Node]

  private val edges =
    mutable.Map.empty[String, Edge]

  def addNode[N <: Node](label: String,
                         data: Map[String, JsValue]) = {
    val id =
      UUID.randomUUID().toString
    val node =
      nodeFactory(id, label, data)(this).asInstanceOf[N]
    nodes += (id -> node)
    node
  }

  def addEdge[E <: Edge](_1: Node,
                         _2: Node,
                         label: String,
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

  def pathsTo(start: Node, end: Node, nodeLabels: Seq[String], edgeLabels: Seq[String]) = ???
}

package com.seancheatham.graph

import play.api.libs.json.JsObject

/**
  * Represents a chain of edges to represent a path through a graph
  *
  * @param startNode The first node in this path
  * @param edges The chain of (sequential) edges in this path
  */
case class Path(startNode: Node,
                edges: Seq[Edge]) {

  /**
    * Extracts the nodes in this path, in order, including [[com.seancheatham.graph.Path#startNode()]]
    */
  def nodes =
    edges.foldLeft(Vector(startNode)) {
      case (seq, edge) =>
        val last =
          seq.last
        if(edge._1 == last)
          seq :+ edge._2
        else
          seq :+ edge._1
    }

  /**
    * Determines the last Node in this path
    */
  def endNode =
    edges match {
      case Seq() =>
        startNode
      case Seq(edge) =>
        if(edge._1 == startNode)
          edge._2
        else
          edge._1
      case _ :+ (penultimate: Edge) :+ (last: Edge) =>
        if(last._1 == penultimate._1 || last._1 == penultimate._2)
          last._2
        else
          last._1
    }

}

object Path {

  def fromJson(json: JsObject)(implicit graph: Graph): Path =
    Path(
      Node.fromJson((json \ "start").as[JsObject]),
      (json \ "path").as[Seq[JsObject]]
        .map(j => Edge.fromJson(j, graph.getNode[Node] _, graph.getNode[Node] _))
    )

}
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

  def bfs(start: Node,
              end: Node,
              nodeLabels: Seq[String],
              edgeLabels: Seq[String]) = {
    import scala.collection.mutable

    val distances =
      mutable.Map.empty[Node, Int]

    val parents =
      mutable.Map.empty[Node, (Edge, Boolean)]

    val queue =
      mutable.Queue[Node](start)

    var continue =
      true

    while (continue && queue.nonEmpty) {

      val current =
        queue.dequeue()

      def f(items: Iterator[Edge],
            direction: Boolean) = {

        val cleanItems =
          items
            .filter(edgeLabels contains _.label)
            .filter(nodeLabels contains _._1.label)
            .filter(nodeLabels contains _._2.label)

        while (continue && cleanItems.hasNext) {
          val edge =
            cleanItems.next()

          val node =
            if (direction)
              edge._2
            else
              edge._1

          if (!distances.contains(node)) {
            distances += (node -> (distances.getOrElse(current, 0) + 1))
            parents += (node -> (edge, direction))
            queue.enqueue(node)
          }

          if (node == end)
            continue = false
        }
      }

      f(current.egressEdges[Edge]().toIterator, direction = true)
      f(current.ingressEdges[Edge]().toIterator, direction = false)
    }

    val trail =
      mutable.Stack[Edge]()

    var node =
      end

    while (node != start) {
      val (edge, direction) =
        parents(node)

      trail.push(edge)

      if (direction)
        node = edge._1
      else
        node = edge._2
    }

    val path =
      Path(start, trail.toVector)

    Vector(path)
  }

}
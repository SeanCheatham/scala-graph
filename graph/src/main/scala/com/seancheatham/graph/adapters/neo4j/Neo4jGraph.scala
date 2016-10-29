package com.seancheatham.graph.adapters.neo4j

import com.seancheatham.graph.{Edge, Graph, Node}
import org.neo4j.driver.v1._
import play.api.libs.json._

import scala.collection.JavaConverters._

class Neo4jGraph(private val driver: Driver) extends Graph {

  private val session =
    driver.session()

  protected def runQuery(query: String): StatementResult =
    session.run(query)

  def addNode[N <: Node](label: String, data: Map[String, JsValue]) = {
    val query = {
      val dataContribution =
        if (data.isEmpty)
          ""
        else
          "{%s}"
            .format(
              data.map {
                case (key, value) =>
                  s"$key:${value.toString}"
              }
                .mkString(",")
            )
      s"CREATE(n:$label$dataContribution) RETURN ID(n) AS __id"
    }
    val resultSet =
      session.run(query)

    val result =
      resultSet.single()

    nodeFactory((result.get("__id").asString, label, data))(Neo4jGraph.this).asInstanceOf[N]
  }

  def addEdge[E <: Edge](_1: Node, _2: Node, label: String, data: Map[String, JsValue]) = {
    val query = {
      val dataContribution =
        if (data.isEmpty)
          ""
        else
          "{%s}"
            .format(
              data.map {
                case (key, value) =>
                  s"$key:${value.toString}"
              }
                .mkString(",")
            )
      s"""CREATE(n)-[e:$label$dataContribution]->(m)
          |WHERE ID(n) = ${_1.id} AND ID(m) = ${_2.id}
          |RETURN ID(e) as __id
       """.stripMargin
    }

    val resultSet =
      session.run(query)

    val result =
      resultSet.single()

    edgeFactory((result.get("__id").asString, label, _1, _2, data))(Neo4jGraph.this).asInstanceOf[E]
  }

  def getNode[N <: Node](id: String) = {
    val query =
      s"MATCH(n) WHERE ID(n) = $id RETURN n, labels(n) as __labels"
    val resultSet =
      session.run(query)

    if (resultSet.hasNext) {

      val result =
        resultSet.single()

      Some(
        nodeFactory(
          id,
          result.get("__labels").asList.asScala.head.toString,
          result.asMap.asScala.toMap mapValues Neo4jGraph.anyRefToJson
        )(this).asInstanceOf[N]
      )
    } else {
      None
    }
  }

  def getNodes[N <: Node](data: Map[String, JsValue]) = {
    val query = {
      val dataContribution =
        if (data.isEmpty)
          ""
        else
          "{%s}"
            .format(
              data.map {
                case (key, value) =>
                  s"$key:${value.toString}"
              }
                .mkString(",")
            )
      s"MATCH(n$dataContribution) RETURN n, ID(n) as __id, LABELS(n) as __labels"
    }

    val resultSet =
      session.run(query)

    new Iterator[N] {
      def hasNext =
        resultSet.hasNext

      def next() = {
        val record =
          resultSet.next()

        nodeFactory(
          record.get("__id").asString,
          record.get("__labels").asList.asScala.head.toString,
          data
        )(Neo4jGraph.this).asInstanceOf[N]
      }
    }
  }

  def getNodes[N <: Node](label: String, data: Map[String, JsValue]) = {
    val query = {
      val dataContribution =
        if (data.isEmpty)
          ""
        else
          "{%s}"
            .format(
              data.map {
                case (key, value) =>
                  s"$key:${value.toString}"
              }
                .mkString(",")
            )
      s"MATCH(n:$label$dataContribution) RETURN n, ID(n) as __id, LABELS(n) as __labels"
    }

    val resultSet =
      session.run(query)

    new Iterator[N] {
      def hasNext =
        resultSet.hasNext

      def next() = {
        val record =
          resultSet.next()

        nodeFactory(
          record.get("__id").asString,
          record.get("__labels").asList.asScala.head.toString,
          (record.asMap.asScala.toMap - "__id" - "__labels")
            .mapValues(Neo4jGraph.anyRefToJson)
        )(Neo4jGraph.this).asInstanceOf[N]
      }
    }
  }

  def getEgressEdges[E <: Edge](node: Node, edgeLabel: Option[String], edgeData: Map[String, JsValue]) = {
    val query = {
      val labelContribution =
        edgeLabel.fold("")(s => s":$s")
      val dataContribution =
        if (edgeData.isEmpty)
          ""
        else
          "{%s}"
            .format(
              edgeData.map {
                case (key, value) =>
                  s"$key:${value.toString}"
              }
                .mkString(",")
            )
      s"""(n)-[e$labelContribution$dataContribution]->(o)
          |WHERE ID(n) = ${node.id}
          |RETURN e, ID(e) as __id, LABELS(e) as __labels, o as __o""".stripMargin
    }

    val resultSet =
      session.run(query)

    new Iterator[E] {
      def hasNext =
        resultSet.hasNext

      def next() = {
        val record =
          resultSet.next()

        edgeFactory(
          record.get("__id").asString,
          record.get("__labels").asList.asScala.head.toString,
          node,
          Node.fromJson(Neo4jGraph.anyRefToJson(record.get("__o").asNode).as[JsObject])(Neo4jGraph.this),
          (record.asMap.asScala.toMap - "__id" - "__labels" - "__o")
            .mapValues(Neo4jGraph.anyRefToJson)
        )(Neo4jGraph.this).asInstanceOf[E]
      }
    }
  }

  def getIngressEdges[E <: Edge](node: Node, edgeLabel: Option[String], edgeData: Map[String, JsValue]) = {
    val query = {
      val labelContribution =
        edgeLabel.fold("")(s => s":$s")
      val dataContribution =
        if (edgeData.isEmpty)
          ""
        else
          "{%s}"
            .format(
              edgeData.map {
                case (key, value) =>
                  s"$key:${value.toString}"
              }
                .mkString(",")
            )
      s"""(o)-[e$labelContribution$dataContribution]->(n)
          |WHERE ID(n) = ${node.id}
          |RETURN e, ID(e) as __id, LABELS(e) as __labels, o as __o""".stripMargin
    }

    val resultSet =
      session.run(query)
    new Iterator[E] {
      def hasNext =
        resultSet.hasNext

      def next() = {
        val record =
          resultSet.next()

        edgeFactory(
          record.get("__id").asString,
          record.get("__labels").asList.asScala.head.toString,
          Node.fromJson(Neo4jGraph.anyRefToJson(record.get("__o").asNode).as[JsObject])(Neo4jGraph.this),
          node,
          (record.asMap.asScala.toMap - "__id" - "__labels" - "__o")
            .mapValues(Neo4jGraph.anyRefToJson)
        )(Neo4jGraph.this).asInstanceOf[E]
      }
    }
  }

}

object Neo4jGraph {
  def apply(address: String): Neo4jGraph =
    new Neo4jGraph(
      GraphDatabase.driver(address)
    )

  def apply(address: String,
            auth: AuthToken): Neo4jGraph =
    new Neo4jGraph(
      GraphDatabase.driver(address, auth)
    )

  def anyRefToJson(r: AnyRef): JsValue =
    Option(r)
      .fold[JsValue](JsNull) {
      case v: String => JsString(v)
      case v: java.lang.Long => JsNumber(BigDecimal(v.toString))
      case v: java.lang.Double => JsNumber(BigDecimal(v.toString))
      case v: java.lang.Number => JsNumber(BigDecimal(v.toString))
      case v: java.lang.Boolean => JsBoolean(v)
      case v: java.util.Map[String, AnyRef] => JsObject(v.asScala.toMap mapValues anyRefToJson)
      case v: java.util.List[AnyRef] => JsArray(v.asScala map anyRefToJson)
      case v: org.neo4j.driver.v1.types.Node =>
        Json.obj(
          "id" -> v.id.toString,
          "type" -> "node",
          "label" -> v.labels.asScala.head,
          "data" -> v.asMap.asScala.mapValues(anyRefToJson)
        )
      case v: org.neo4j.driver.v1.types.Relationship =>
        Json.obj(
          "id" -> v.id.toString,
          "type" -> "edge",
          "label" -> v.`type`,
          "data" -> v.asMap.asScala.mapValues(anyRefToJson),
          "_1" -> v.startNodeId.toString,
          "_2" -> v.endNodeId.toString
        )
      case v: org.neo4j.driver.v1.types.Path =>
        ???
    }
}

package com.seancheatham.graph.adapters.neo4j

import com.seancheatham.graph.{Edge, Graph, Node}
import org.neo4j.driver.v1._
import play.api.libs.json.{JsObject, _}

import scala.collection.JavaConverters._

class Neo4jGraph(private val driver: Driver)
                (override implicit val nodeFactory: Node.Factory = Node.defaultFactory,
                 override implicit val edgeFactory: Edge.Factory = Edge.defaultFactory) extends Graph {

  import com.seancheatham.graph.adapters.neo4j.Neo4jGraph._

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
          jsValueToNeo4j(JsObject(data))
      s"CREATE (n:`$label`$dataContribution) RETURN n"
    }
    val resultSet =
      session.run(query)

    val result =
      resultSet.single()

    Node.fromJson(
      anyRefToJson(
        result.get("n").asObject
      ).as[JsObject]
    ).asInstanceOf[N]
  }

  def addEdge[E <: Edge](_1: Node, _2: Node, label: String, data: Map[String, JsValue]) = {
    val query = {
      val dataContribution =
        if (data.isEmpty)
          ""
        else
          jsValueToNeo4j(JsObject(data))
      s"""MATCH (n)
          |WHERE ID(n) = ${_1.id}
          |MATCH (m)
          |WHERE ID(m) = ${_2.id}
          |CREATE (n)-[e:`$label`$dataContribution]->(m)
          |RETURN e
       """.stripMargin
    }

    val resultSet =
      session.run(query)

    val result =
      resultSet.single()

    Edge.fromJson(
      anyRefToJson(
        result.get("e").asObject
      ).as[JsObject],
      _1,
      _2
    ).asInstanceOf[E]
  }

  def getNode[N <: Node](id: String) = {
    val query =
      s"MATCH (n) WHERE ID(n) = $id RETURN n"

    val resultSet =
      session.run(query)

    if (resultSet.hasNext) {
      val result =
        resultSet.single()

      Some(
        Node.fromJson(
          anyRefToJson(
            result.get("n").asObject
          ).as[JsObject]
        ).asInstanceOf[N]
      )
    } else {
      None
    }
  }

  def getNodes[N <: Node](label: Option[String] = None,
                          data: Map[String, JsValue] = Map.empty) = {
    val query = {
      val labelContribution =
        label.fold("")(l => s":`$l`")
      val dataContribution =
        if (data.isEmpty)
          ""
        else
          jsValueToNeo4j(JsObject(data))
      s"MATCH (n$labelContribution$dataContribution) RETURN n"
    }

    val resultSet =
      session.run(query)

    new Iterator[N] {
      def hasNext =
        resultSet.hasNext

      def next() = {
        val record =
          resultSet.next()

        Node.fromJson(
          anyRefToJson(
            record.get("n").asObject
          ).as[JsObject]
        ).asInstanceOf[N]
      }
    }
  }

  def getEgressEdges[E <: Edge](node: Node,
                                edgeLabel: Option[String] = None,
                                edgeData: Map[String, JsValue] = Map.empty) = {
    val query = {
      val labelContribution =
        edgeLabel.fold("")(s => s":`$s`")
      val dataContribution =
        if (edgeData.isEmpty)
          ""
        else
          jsValueToNeo4j(JsObject(edgeData))
      s"""MATCH (n)-[e$labelContribution$dataContribution]->(o)
          |WHERE ID(n) = ${node.id}
          |RETURN e, o""".stripMargin
    }

    val resultSet =
      session.run(query)

    new Iterator[E] {
      def hasNext =
        resultSet.hasNext

      def next() = {
        val record =
          resultSet.next()

        Edge.fromJson(
          anyRefToJson(
            record.get("e").asObject
          ).as[JsObject],
          node,
          Node.fromJson(
            anyRefToJson(
              record.get("o").asObject
            ).as[JsObject]
          )
        ).asInstanceOf[E]

      }
    }
  }

  def getIngressEdges[E <: Edge](node: Node,
                                 edgeLabel: Option[String] = None,
                                 edgeData: Map[String, JsValue] = Map.empty) = {
    val query = {
      val labelContribution =
        edgeLabel.fold("")(s => s":`$s`")
      val dataContribution =
        if (edgeData.isEmpty)
          ""
        else
          jsValueToNeo4j(JsObject(edgeData))
      s"""MATCH (o)-[e$labelContribution$dataContribution]->(n)
          |WHERE ID(n) = ${node.id}
          |RETURN e, o""".stripMargin
    }

    val resultSet =
      session.run(query)
    new Iterator[E] {
      def hasNext =
        resultSet.hasNext

      def next() = {
        val record =
          resultSet.next()

        Edge.fromJson(
          anyRefToJson(
            record.get("e").asObject
          ).as[JsObject],
          Node.fromJson(
            anyRefToJson(
              record.get("o").asObject
            ).as[JsObject]
          ),
          node
        ).asInstanceOf[E]
      }
    }
  }

  def removeNode(node: Node) = {
    val query =
      s"MATCH (n) WHERE ID(n) = ${node.id} DETACH DELETE n"

    session.run(query)

    this
  }

  def removeNodes(label: Option[String] = None,
                  data: Map[String, JsValue] = Map.empty) = {
    val query = {
      val labelContribution =
        label.fold("")(s => s":`$s`")
      val dataContribution =
        if (data.isEmpty)
          ""
        else
          jsValueToNeo4j(JsObject(data))
      s"MATCH (n$labelContribution$dataContribution) DETACH DELETE n"
    }

    session.run(query)

    this
  }

  def removeEdge(edge: Edge): Graph = {
    val query =
      s"MATCH ()-[e]->() WHERE ID(e) = ${edge.id} DELETE e"

    session.run(query)

    this
  }

  def updateNode[N <: Node](node: N)(changes: (String, JsValue)*) = {
    val query = {
      val setter = {
        val data =
          jsValueToNeo4j(JsObject(changes.toMap))
        s"SET n += $data"
      }
      s"""MATCH (n)
          |WHERE ID(n) = ${node.id}
          |$setter
          |RETURN n
       """.stripMargin
    }

    val resultSet =
      session.run(query)

    val result =
      resultSet.single()

    Node.fromJson(
      anyRefToJson(
        result.get("n").asObject
      ).as[JsObject]
    ).asInstanceOf[N]
  }

  def updateEdge[E <: Edge](edge: E)(changes: (String, JsValue)*) = {
    val query = {
      val setter = {
        val data =
          jsValueToNeo4j(JsObject(changes.toMap))
        s"SET e += $data"
      }
      s"""MATCH ()-[e]->()
          |WHERE ID(e) = ${edge.id}
          |$setter
          |RETURN e
       """.stripMargin
    }

    session.run(query)

    val updatedData =
      (edge.data ++ (changes filterNot (_._2 == JsNull))) --
        changes.collect { case (key, JsNull) => key }

    edgeFactory((edge.id, edge.label, edge._1, edge._2, updatedData))(Neo4jGraph.this).asInstanceOf[E]
  }

}

object Neo4jGraph {
  def apply(address: String)
           (implicit nodeFactory: Node.Factory,
            edgeFactory: Edge.Factory): Neo4jGraph =
    new Neo4jGraph(
      GraphDatabase.driver(address)
    )

  def apply(address: String,
            auth: AuthToken)
           (implicit nodeFactory: Node.Factory,
            edgeFactory: Edge.Factory): Neo4jGraph =
    new Neo4jGraph(
      GraphDatabase.driver(address, auth)
    )

  def anyRefToJson(r: AnyRef): JsValue =
    Option(r)
      .fold[JsValue](JsNull) {
      case v: String => JsString(v)
      case v: java.lang.Long =>
        JsNumber(BigDecimal(v.toString))
      case v: java.lang.Double =>
        JsNumber(BigDecimal(v.toString))
      case v: java.lang.Number =>
        JsNumber(BigDecimal(v.toString))
      case v: java.lang.Boolean =>
        JsBoolean(v)
      case v: java.util.Map[String@unchecked, AnyRef@unchecked] =>
        JsObject(v.asScala.toMap mapValues anyRefToJson)
      case v: java.util.List[AnyRef@unchecked] =>
        JsArray(v.asScala map anyRefToJson)
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

  def jsValueToNeo4j(v: JsValue): String =
    v match {
      case JsNull =>
        "NULL"
      case value: JsBoolean =>
        Json.stringify(value).toUpperCase
      case value: JsObject =>
        val data =
          value.value.map {
            case (key, value1) =>
              s"`$key`:${jsValueToNeo4j(value1)}"
          }
            .mkString(",")
        s"{$data}"
      case value: JsArray =>
        val data =
          value.value map jsValueToNeo4j mkString ","
        s"{$data}"
      case value =>
        Json.stringify(value)
    }
}

package com.seancheatham.graph.adapters.neo4j

import java.io.File
import java.util.UUID

import com.seancheatham.graph.{Edge, Graph, Node, Path}
import org.neo4j.driver.v1._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.{Direction, GraphDatabaseService, Label, RelationshipType}
import play.api.libs.json.{JsObject, _}

import scala.collection.JavaConverters._
import scala.util.Try

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

  def pathsTo(start: Node,
              end: Node,
              nodeLabels: Seq[String] = Seq.empty,
              edgeLabels: Seq[String] = Seq.empty) = {
    val query = {
      val nodeFilterContribution =
        if(nodeLabels.nonEmpty)
          s"WHERE ALL(x IN NODES(path) WHERE x${nodeLabels.map(":" + _).mkString})"
        else
          ""

      s"""MATCH (start{id:${start.id}}),
          | (end{id:${end.id}}),
          | path =(start)-[${edgeLabels.map(":" + _).mkString}*]-(end)
          | $nodeFilterContribution
          |RETURN path
       """.stripMargin
    }

    val resultSet =
      session.run(query)

    new Iterator[Path] {
      def hasNext =
        resultSet.hasNext
      def next() = {
        val record =
          resultSet.next()
        Path.fromJson(
          anyRefToJson(
            record.get("path").asPath
          ).as[JsObject]
        )
      }
    }
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

  def embedded(directory: String = s"/tmp/${UUID.randomUUID().toString}") =
    new EmbeddedNeo4jGraph(
      new GraphDatabaseFactory()
        .newEmbeddedDatabase(new File(directory))
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
      case v: org.neo4j.kernel.impl.core.NodeProxy =>
        Json.obj(
          "id" -> v.getId.toString,
          "type" -> "node",
          "label" -> v.getLabels.asScala.head.name,
          "data" -> v.getAllProperties.asScala.mapValues(anyRefToJson)
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
      case v: org.neo4j.kernel.impl.core.RelationshipProxy =>
        Json.obj(
          "id" -> v.getId.toString,
          "type" -> "edge",
          "label" -> v.getType.name,
          "data" -> v.getAllProperties.asScala.mapValues(anyRefToJson),
          "_1" -> v.getStartNode.getId.toString,
          "_2" -> v.getEndNode.getId.toString
        )
      case v: org.neo4j.driver.v1.types.Path =>
        Json.obj(
          "start" -> anyRefToJson(v.start),
          "path" -> v.asScala.map(s => anyRefToJson(s.relationship))
        )
    }

  def jsValueToAny(v: JsValue): Option[Any] =
    v match {
      case value: JsBoolean =>
        Some(value.value)
      case value: JsObject =>
        // TODO: Fix me
        val data =
          value.value.map {
            case (key, value1) =>
              s"`$key`:${jsValueToAny(value1)}"
          }
            .mkString(",")
        Some(s"{$data}")
      case value: JsArray =>
        Some((value.value map jsValueToAny).toArray)
      case value: JsString =>
        Some(value.value)
      case value: JsNumber =>
        Some(value.as[Float])
      case JsNull =>
        None
    }

  def jsValueToNeo4j(v: JsValue): Any =
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

class EmbeddedNeo4jGraph(private val service: GraphDatabaseService)
                        (override implicit val nodeFactory: Node.Factory = Node.defaultFactory,
                         override implicit val edgeFactory: Edge.Factory = Edge.defaultFactory) extends Graph {

  import Neo4jGraph._

  protected def runInTransaction[T](actions: => T) =
    Try(service.beginTx())
      .map { t =>
        val result =
          actions
        t.success()
        result
      }

  def addNode[N <: Node](label: String, data: Map[String, JsValue]) =
    runInTransaction {
      val node = service.createNode(Label.label(label))
      data.foreach {
        case (key, value) =>
          jsValueToAny(value)
            .fold[Unit](node.removeProperty(key))(node.setProperty(key, _))
      }
      node.getId.toString
    }
      .map(getNode[N])
      .get.get

  def addEdge[E <: Edge](_1: Node, _2: Node, label: String, data: Map[String, JsValue]) =
    runInTransaction {
      val n1 = service.getNodeById(_1.id.toLong)
      val n2 = service.getNodeById(_2.id.toLong)
      val rel = n1.createRelationshipTo(n2, RelationshipType.withName(label))
      data.foreach {
        case (key, value) =>
          jsValueToAny(value)
            .fold[Unit](rel.removeProperty(key))(rel.setProperty(key, _))
      }
      edgeFactory(rel.getId.toString, label, _1, _2, data)(this).asInstanceOf[E]
    }
      .get

  def getNode[N <: Node](id: String) =
    runInTransaction {
      Try(service.getNodeById(id.toLong))
        .toOption
        .map(n =>
          Node.fromJson(
            anyRefToJson(n).as[JsObject]
          )(this).asInstanceOf[N]
        )
    }
      .get

  def getNodes[N <: Node](label: Option[String] = None,
                          data: Map[String, JsValue] = Map.empty) =
    runInTransaction {
      val nodes =
        service.findNodes(Label.label(label.getOrElse("DEFAULT")))
      nodes.asScala
        .map(anyRefToJson)
        .map(_.as[JsObject])
        .map(Node.fromJson(_)(this))
        .filter(n => data forall (d => n.data(d._1) == d._2))
        .map(_.asInstanceOf[N])
    }
      .get

  def getEgressEdges[E <: Edge](node: Node,
                                edgeLabel: Option[String] = None,
                                edgeData: Map[String, JsValue] = Map.empty) =
    runInTransaction {
      Try(service.getNodeById(node.id.toLong))
        .map {
          n =>
            val relationships =
              edgeLabel.fold(
                n.getRelationships(Direction.OUTGOING)
              )(eLabel => n.getRelationships(Direction.OUTGOING, RelationshipType.withName(eLabel)))
                .asScala
                .iterator

            relationships
              .map(anyRefToJson)
              .map(_.as[JsObject])
              .map(Edge.fromJson(_, getNode _, getNode _)(this).asInstanceOf[E])
        }
        .getOrElse(Iterator.empty)
    }
      .get

  def getIngressEdges[E <: Edge](node: Node,
                                 edgeLabel: Option[String] = None,
                                 edgeData: Map[String, JsValue] = Map.empty) =
    runInTransaction {
      Try(service.getNodeById(node.id.toLong))
        .map {
          n =>
            val relationships =
              edgeLabel.fold(
                n.getRelationships(Direction.INCOMING)
              )(eLabel => n.getRelationships(Direction.INCOMING, RelationshipType.withName(eLabel)))
                .asScala
                .iterator

            relationships
              .map(anyRefToJson)
              .map(_.as[JsObject])
              .map(Edge.fromJson(_, getNode _, getNode _)(this).asInstanceOf[E])
        }
        .getOrElse(Iterator.empty)
    }
      .get

  def removeNode(node: Node) =
    runInTransaction {
      Try(service.getNodeById(node.id.toLong))
        .map {
          n =>
            n.delete()
        }
      this
    }
      .getOrElse(this)

  def removeNodes(label: Option[String] = None,
                  data: Map[String, JsValue] = Map.empty) =
    runInTransaction {
      val nodes =
        service.findNodes(Label.label(label.getOrElse("DEFAULT")))
      nodes.asScala
        .map(n => n -> anyRefToJson(n).as[JsObject])
        .toMap
        .mapValues(_.as[JsObject])
        .filter(n => data forall (d => (n._2 \ "data" \ d._1).toOption contains d._2))
        .foreach(_._1.delete())
      this
    }
      .getOrElse(this)

  def removeEdge(edge: Edge): Graph =
    runInTransaction {
      Try(service.getRelationshipById(edge.id.toLong))
        .map(_.delete)
      this
    }
      .getOrElse(this)

  def updateNode[N <: Node](node: N)(changes: (String, JsValue)*) =
    runInTransaction {
      Try(service.getNodeById(node.id.toLong))
        .map {
          n =>
            changes.foreach {
              case (key, value) =>
                jsValueToAny(value)
                  .fold[Unit](n.removeProperty(key))(n.setProperty(key, _))
            }
            Node.fromJson(
              anyRefToJson(n).as[JsObject]
            )(this).asInstanceOf[N]
        }
        .get
    }
      .get

  def updateEdge[E <: Edge](edge: E)(changes: (String, JsValue)*) =
    runInTransaction {
      Try(service.getRelationshipById(edge.id.toLong))
        .map {
          rel =>
            changes.foreach {
              case (key, value) =>
                jsValueToAny(value)
                  .fold[Unit](rel.removeProperty(key))(rel.setProperty(key, _))
            }

            Edge.fromJson(
              anyRefToJson(rel).as[JsObject],
              getNode[Node] _,
              getNode[Node] _
            )(this).asInstanceOf[E]
        }
        .get
    }
      .get

  def pathsTo(start: Node,
              end: Node,
              nodeLabels: Seq[String] = Seq.empty,
              edgeLabels: Seq[String] = Seq.empty) =
    ???

}
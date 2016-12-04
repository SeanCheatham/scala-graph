package com.seancheatham.graph.adapters.hbase

import java.util.UUID

import com.seancheatham.graph.{Edge, Graph, Node, Path}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp
import org.apache.hadoop.hbase.filter.{Filter, FilterList, SingleColumnValueFilter}
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * A Graph adapter which connects to a HBase instance via zookeeper.  Uses the HBase API to access it.
  *
  * @param connection     An HBase connection
  * @param nodesTableName The name of the table housing the nodes
  * @param edgesTableName The name of the table housing the edges
  */
class HBaseGraph(connection: Connection,
                 nodesTableName: String = "nodes",
                 edgesTableName: String = "edges") extends Graph with LazyLogging {

  import HBaseGraph._

  private val admin =
    connection.getAdmin

  private val nTName =
    TableName.valueOf(nodesTableName.getBytes)

  private val eTName =
    TableName.valueOf(edgesTableName.getBytes)

  Try(admin.getTableDescriptor(nTName)) match {
    case Failure(_: TableNotFoundException) =>
      logger.info(s"Nodes table $nodesTableName did not exist; creating.")
      admin.createTable(
        new HTableDescriptor(nTName)
          .addFamily(new HColumnDescriptor(metaFamilyName))
          .addFamily(new HColumnDescriptor(dataFamilyName))
      )
      logger.info(s"Nodes table $nodesTableName created.")
      admin.getTableDescriptor(nTName)
    case Failure(e) =>
      throw e
    case Success(td) =>
      td
  }

  Try(admin.getTableDescriptor(eTName)) match {
    case Failure(e: TableNotFoundException) =>
      logger.info(s"Edges table $edgesTableName did not exist; creating.")
      admin.createTable(
        new HTableDescriptor(eTName)
          .addFamily(new HColumnDescriptor(metaFamilyName))
          .addFamily(new HColumnDescriptor(dataFamilyName))
      )
      logger.info(s"Edges table $edgesTableName created.")
      admin.getTableDescriptor(eTName)
    case _ =>
      admin.getTableDescriptor(eTName)
    case Failure(e) =>
      throw e
    case Success(td) =>
      td
  }

  private val nodesTable =
    connection.getTable(nTName)

  private val edgesTable =
    connection.getTable(eTName)

  def shutdown(): Unit = {
    nodesTable.close()
    edgesTable.close()
    connection.close()
  }

  def addNode[N <: Node](label: String, data: Map[String, JsValue]): N = {
    val id =
      UUID.randomUUID().toString
    val put =
      data.flatList()
        .foldLeft(
          new Put(id.getBytes)
            .addColumn(metaFamilyName, "label".getBytes, label.getBytes)
        ) {
          case (p, (key, value)) =>
            p.addColumn(dataFamilyName, key.getBytes, value.toString.getBytes)
        }
    nodesTable.put(put)
    getNode[N](id).get
  }

  def addEdge[E <: Edge](label: String, _1: Node, _2: Node, data: Map[String, JsValue]): E = {
    val id =
      UUID.randomUUID().toString
    val put =
      data.flatList()
        .foldLeft(
          new Put(id.getBytes)
            .addColumn(metaFamilyName, "label".getBytes, label.getBytes)
            .addColumn(metaFamilyName, "_1".getBytes, _1.id.getBytes)
            .addColumn(metaFamilyName, "_2".getBytes, _2.id.getBytes)
        ) {
          case (p, (key, value)) =>
            p.addColumn(dataFamilyName, key.getBytes, value.toString.getBytes)
        }
    edgesTable.put(put)
    getEdge[E](id).get
  }

  def getNode[N <: Node](id: String): Option[N] = {
    val result =
      nodesTable.get(new Get(id.getBytes))
    if (result.isEmpty)
      None
    else
      Some(nodeFactory(result.asNodeConstruct)(this).asInstanceOf[N])
  }

  def getNodes[N <: Node](label: Option[String], data: Map[String, JsValue]): TraversableOnce[N] = {
    val scan =
      ScanHelper.withFilterLabelData(label, data)
    val results =
      nodesTable.getScanner(scan)
    results.asScala
      .map(_.asNodeConstruct)
      .map(nodeFactory(_)(this))
      .map(_.asInstanceOf[N])
  }

  def getEdge[E <: Edge](id: String): Option[E] = {
    val result =
      edgesTable.get(new Get(id.getBytes))
    if (result.isEmpty)
      None
    else
      Some(
        edgeFactory(result.asEdgeConstruct)(this).asInstanceOf[E]
      )
  }

  def getEdges[E <: Edge](label: Option[String], data: Map[String, JsValue]): TraversableOnce[E] = {
    val scan =
      ScanHelper.withFilterLabelData(label, data)
    val results =
      edgesTable.getScanner(scan)
    results.asScala
      .map(_.asEdgeConstruct)
      .map(edgeFactory(_)(this))
      .map(_.asInstanceOf[E])
  }

  def getEgressEdges[E <: Edge](node: Node, edgeLabel: Option[String], edgeData: Map[String, JsValue]): TraversableOnce[E] = {
    val scan =
      new Scan()
        .setFilter {
          val lFilter =
            edgeLabel.map(ScanHelper.labelFilter)
          val dFilters =
            ScanHelper.dataFilters(edgeData)
          val _1Filter =
            new SingleColumnValueFilter(metaFamilyName, "_1".getBytes, CompareOp.EQUAL, node.id.getBytes)
          new FilterList((lFilter.toList ++ dFilters :+ _1Filter).asJava)
        }
    val results =
      edgesTable.getScanner(scan)
    results.asScala
      .map(_.asEdgeConstruct)
      .map(edgeFactory(_)(this))
      .map(_.asInstanceOf[E])
  }

  def getIngressEdges[E <: Edge](node: Node, edgeLabel: Option[String], edgeData: Map[String, JsValue]): TraversableOnce[E] = {
    val scan =
      new Scan()
        .setFilter {
          val lFilter =
            edgeLabel.map(ScanHelper.labelFilter)
          val dFilters =
            ScanHelper.dataFilters(edgeData)
          val _2Filter =
            new SingleColumnValueFilter(metaFamilyName, "_2".getBytes, CompareOp.EQUAL, node.id.getBytes)
          new FilterList((lFilter.toList ++ dFilters :+ _2Filter).asJava)
        }
    val results =
      edgesTable.getScanner(scan)
    results.asScala
      .map(_.asEdgeConstruct)
      .map(edgeFactory(_)(this))
      .map(_.asInstanceOf[E])
  }

  def removeNode(node: Node): Graph = {
    (node.egressEdges().toIterator ++
      node.ingressEdges())
      .foreach(removeEdge)
    nodesTable.delete(new Delete(node.id.getBytes))
    this
  }

  def removeNodes(label: Option[String], data: Map[String, JsValue]): Graph = {
    getNodes[Node](label, data)
      .foreach(removeNode)
    this
  }

  def removeEdge(edge: Edge): Graph = {
    edgesTable.delete(new Delete(edge.id.getBytes))
    this
  }

  def updateNode[N <: Node](node: N)(changes: (String, JsValue)*): N = {
    val put =
      changes.toMap.flatList()
        .foldLeft(new Put(node.id.getBytes)) {
          case (p, (key, value)) =>
            p.addColumn(dataFamilyName, key.getBytes, value.toString.getBytes)
        }
    nodesTable.put(put)
    getNode[N](node.id).get
  }

  def updateEdge[E <: Edge](edge: E)(changes: (String, JsValue)*): E = {
    val put =
      changes.toMap.flatList()
        .foldLeft(new Put(edge.id.getBytes)) {
          case (p, (key, value)) =>
            p.addColumn(dataFamilyName, key.getBytes, value.toString.getBytes)
        }
    edgesTable.put(put)
    getEdge[E](edge.id).get
  }

  def pathsTo(start: Node, end: Node, nodeLabels: Seq[String], edgeLabels: Seq[String]): TraversableOnce[Path] =
    Path.bfs(start, end, nodeLabels, edgeLabels)
}

object HBaseGraph {

  def fromConf(nodesTableName: String = "nodes",
               edgesTableName: String = "edges")
              (implicit config: Config) = {
    val zookeeperAddress =
      config.getString("graph.hbase.zookeeper.address")
    val zookeeperPort =
      config.getInt("graph.hbase.zookeeper.port")
    val hbaseConfig =
      HBaseConfiguration.create()
    hbaseConfig.set("hbase.zookeeper.property.clientPort", zookeeperPort.toString)
    hbaseConfig.set("hbase.zookeeper.quorum", zookeeperAddress)
    // TODO: Needed?
    //    hbaseConfig.set("zookeeper.znode.parent", "/hbase-unsecure")
    val connection =
    ConnectionFactory.createConnection(hbaseConfig)

    new HBaseGraph(connection, nodesTableName, edgesTableName)

  }

  protected val metaFamilyName: Array[Byte] =
    "meta".getBytes

  protected val dataFamilyName: Array[Byte] =
    "data".getBytes

  object ScanHelper {

    def dataFilters(data: Map[String, JsValue],
                    prefix: String = "data."): Iterable[Filter] =
      data.flatList(prefix)
        .map {
          case (key, value) =>
            new SingleColumnValueFilter(metaFamilyName, key.getBytes, CompareOp.EQUAL, value.toString.getBytes)
        }

    def labelFilter(label: String) =
      new SingleColumnValueFilter(metaFamilyName, "label".getBytes, CompareOp.EQUAL, label.getBytes)

    def withFilterLabelData(label: Option[String], data: Map[String, JsValue]): Scan = {
      val filterList =
        new FilterList((label.map(labelFilter) ++ dataFilters(data)).toList.asJava)
      new Scan().setFilter(filterList)
    }
  }

  implicit class JsMapHelper(data: Map[String, JsValue]) {
    def flatList(prefix: String = "data."): Map[String, JsValue] =
      data.flatMap {
        case (key, value: JsObject) =>
          value.value.toMap.flatList(prefix + key + ".")
        case (key, value) =>
          Vector(
            (prefix + key) -> value
          )
      }

    def unflattenList(prefix: String = "data."): Map[String, JsValue] =
      data
        .map { case (key, value) => key.substring(prefix.length) -> value }
        .flatMap {
          case (key, value) =>
            key.split('.')
              .foldRight(value) {
                case (part, v) =>
                  Json.obj(part -> v)
              }
              .as[Map[String, JsValue]]
        }
  }

  implicit class CellHelper(cell: Cell) {
    def asData =
      Json.parse(cell.getValueArray).as[Map[String, JsValue]]

    def asString =
      new String(cell.getValueArray)
  }

  implicit class ResultHelper(result: Result) {
    def asNodeConstruct: (String, String, Map[String, JsValue]) = {
      val id =
        new String(result.getRow)
      val label =
        new String(result.getValue(metaFamilyName, "label".getBytes))
      val data =
        result.getFamilyMap(dataFamilyName).asScala
          .map {
            case (key, value) => new String(key) -> Json.parse(value)
          }
          .toMap
          .unflattenList()
      (id, label, data)
    }

    def asEdgeConstruct(implicit graph: Graph): (String, String, Node, Node, Map[String, JsValue]) = {
      val id =
        new String(result.getRow)
      val label =
        new String(result.getValue(metaFamilyName, "label".getBytes))
      val _1Id =
        new String(result.getValue(metaFamilyName, "_1".getBytes))
      val _2Id =
        new String(result.getValue(metaFamilyName, "_2".getBytes))
      val data =
        result.getFamilyMap(dataFamilyName).asScala
          .map {
            case (key, value) => new String(key) -> Json.parse(value)
          }
          .toMap
          .unflattenList()
      (id, label, graph.getNode[Node](_1Id).get, graph.getNode[Node](_2Id).get, data)
    }
  }

}

package com.seancheatham.graph.adapters.bigtable

import java.util.UUID

import com.google.cloud.bigtable.hbase.{BigtableConfiguration, BigtableOptionsFactory}
import com.seancheatham.graph.adapters.hbase.HBaseGraph
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.Connection

/**
  * An adapter which turns a Google Big Table into a graph database.  Though databases like Bigtable are extremely
  * performant across large datasets for fast individual record reads, they are less performant for graph pathing
  * algorithms.  As such, Big Table is best suited for data which best leverages "Graph Representation", but not
  * necessarily the accompanying algorithms to traverse them.
  */
class BigtableGraph(connection: Connection,
                    nodesTableName: String = "nodes",
                    edgesTableName: String = "edges")
  extends HBaseGraph(connection, nodesTableName, edgesTableName)

object BigtableGraph {
  def apply(projectId: String,
            instanceId: String,
            nodesTableName: String = "nodes",
            edgesTableName: String = "edges"): BigtableGraph =
    new BigtableGraph(
      BigtableConfiguration.connect(
        projectId,
        instanceId
      ),
      nodesTableName,
      edgesTableName
    )

  def fromHost(dataHost: String,
               adminHost: String,
               port: Int,
               nodesTableName: String = "nodes",
               edgesTableName: String = "edges"): BigtableGraph = {
    val config =
      new Configuration(false)
    config.set(BigtableOptionsFactory.BIGTABLE_HOST_KEY, dataHost)
    config.set(BigtableOptionsFactory.BIGTABLE_TABLE_ADMIN_HOST_KEY, dataHost)
    config.setInt(BigtableOptionsFactory.BIGTABLE_PORT_KEY, port)
    config.setBoolean(BigtableOptionsFactory.BIGTABLE_USE_PLAINTEXT_NEGOTIATION, true)
    config.setBoolean(BigtableOptionsFactory.BIGTABLE_NULL_CREDENTIAL_ENABLE_KEY, true)
    config.set(BigtableOptionsFactory.PROJECT_ID_KEY, UUID.randomUUID().toString)
    config.set(BigtableOptionsFactory.INSTANCE_ID_KEY, UUID.randomUUID().toString)
    val connection =
      BigtableConfiguration.connect(config)
    new BigtableGraph(connection, nodesTableName, edgesTableName)
  }
}
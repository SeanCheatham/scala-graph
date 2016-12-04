package com.seancheatham.graph.adapters.bigtable

import com.google.cloud.bigtable.hbase.BigtableConfiguration
import com.seancheatham.graph.adapters.hbase.HBaseGraph

/**
  * An adapter which turns a Google Big Table into a graph database.  Though databases like BigTable are extremely
  * performant across large datasets for fast individual record reads, they are less performant for graph pathing
  * algorithms.  As such, Big Table is best suited for data which best leverages "Graph Representation", but not
  * necessarily the accompanying algorithms to traverse them.
  */
class BigTableGraph(projectId: String,
                    instanceId: String,
                    nodesTableName: String = "nodes",
                    edgesTableName: String = "edges")
  extends HBaseGraph(
    BigtableConfiguration.connect(
      projectId,
      instanceId
    ),
    nodesTableName,
    edgesTableName
  )
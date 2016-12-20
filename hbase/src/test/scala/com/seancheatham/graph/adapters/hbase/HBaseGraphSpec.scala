package com.seancheatham.graph.adapters.hbase

import com.typesafe.config.ConfigFactory
import fixtures.GraphTest

// TODO: Although this test passes when a proper HBase connection can be made, I haven't found a simple way of mocking
// HBase.  Therefore, this test is disabled so that CI can continue.
/*
class HBaseGraphSpec extends GraphTest {
  private val config =
    ConfigFactory.load()
      .withFallback(
        ConfigFactory.parseString("graph.hbase.zookeeper.address=\"hbase-docker\"")
          .withFallback(
            ConfigFactory.parseString("graph.hbase.zookeeper.port=2181")
          )
      )

  val graph: HBaseGraph =
    HBaseGraph.fromConf()(config)

  "Can be shutdown" in {
    graph.admin.disableTable(graph.eTName)
    graph.admin.deleteTable(graph.eTName)
    graph.admin.disableTable(graph.nTName)
    graph.admin.deleteTable(graph.nTName)
    graph.shutdown()
    assert(true)
  }

}
*/
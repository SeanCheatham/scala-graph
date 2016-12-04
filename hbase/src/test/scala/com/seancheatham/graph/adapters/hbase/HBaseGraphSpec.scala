package com.seancheatham.graph.adapters.hbase

import com.typesafe.config.ConfigFactory
import fixtures.GraphTest

class HBaseGraphSpec extends GraphTest {
  private val config =
    ConfigFactory.load()

  val graph: HBaseGraph =
    HBaseGraph.fromConf()(config)

  "Can be shutdown" in {
    graph.shutdown()
    assert(true)
  }

}

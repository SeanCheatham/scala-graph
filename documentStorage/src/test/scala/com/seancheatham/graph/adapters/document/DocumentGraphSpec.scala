package com.seancheatham.graph.adapters.document

import com.seancheatham.graph.Graph
import com.seancheatham.storage.firebase.FirebaseDatabase
import fixtures.GraphTest

class DocumentGraphSpec extends GraphTest {

  val graph: Graph =
    new DocumentGraph(
      FirebaseDatabase(),
      "testing/graph"
    )
}

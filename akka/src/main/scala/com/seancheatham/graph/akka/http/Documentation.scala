package com.seancheatham.graph.akka.http

object Documentation {

  /**
    * The documentation provided to the user at the base "/" path
    */
  final val html: String = {
    def ulBuilder(items: String*) =
      "<ul>\n" +
        items.map(i => s"  <li>$i</li>\n").mkString +
        "</ul>"

    def pathBuilder(name: String,
                    description: String,
                    parameters: Seq[String],
                    responses: Seq[String]) =
      s"""<h4>$name</h4>
         |<i>$description</i>
         |<h5>Parameters</h5>
         |${ulBuilder(parameters: _*)}
         |<h5>Responses</h5>
         |${ulBuilder(responses: _*)}
      """.stripMargin

    val header =
      s"""<h1>HTTP Graph Layer</h1>
         |<h2>API Usage</h2>""".stripMargin

    val routes =
      Seq(
        "<h3>Routes</h3>",
        pathBuilder(
          "GET - /nodes",
          "Fetch a stream of nodes by optional label and optional data",
          Seq(
            "Query(\"label\") - Optional label filter",
            "Body - Optional object of node data filters (key -> value)"
          ),
          Seq(
            "200 - Model: Nodes"
          )
        ),
        pathBuilder(
          "POST - /nodes",
          "Insert a new node with the given label and data",
          Seq(
            "Query(\"label\") - Required label",
            "Body - Object of data for the node"
          ),
          Seq(
            "200 - Model: Node"
          )
        ),
        pathBuilder(
          "GET - /nodes/{id}",
          "Get a node by ID",
          Seq(
            "Path(\"id\") - The ID of the node"
          ),
          Seq(
            "200 - Model: Node",
            "404 - Node could not be found"
          )
        ),
        pathBuilder(
          "PUT - /nodes/{id}",
          "Update a node by ID",
          Seq(
            "Path(\"id\") - The ID of the node",
            "Body - Object of data to upsert into the node"
          ),
          Seq(
            "200 - Model: Node",
            "404 - Node could not be found"
          )
        ),
        pathBuilder(
          "DELETE - /nodes/{id}",
          "Delete a node by ID",
          Seq(
            "Path(\"id\") - The ID of the node"
          ),
          Seq(
            "204 - (Empty Body)",
            "404 - Node could not be found"
          )
        ),
        pathBuilder(
          "GET - /nodes/{id}/edges",
          "Fetch a stream of ingress and egress edges for the given node",
          Seq(
            "Path(\"id\") - The ID of the node",
            "Query(\"label\") - Optional label filter",
            "Body - Optional object of edge data filters (key -> value)"
          ),
          Seq(
            "200 - Edges"
          )
        ),
        pathBuilder(
          "GET - /nodes/{id}/edges/ingress",
          "Fetch a stream of ingress edges for the given node",
          Seq(
            "Path(\"id\") - The ID of the node",
            "Query(\"label\") - Optional label filter",
            "Body - Optional object of edge data filters (key -> value)"
          ),
          Seq(
            "200 - Edges"
          )
        ),
        pathBuilder(
          "GET - /nodes/{id}/edges/egress",
          "Fetch a stream of ingress edges for the given node",
          Seq(
            "Path(\"id\") - The ID of the node",
            "Query(\"label\") - Optional label filter",
            "Body - Optional object of edge data filters (key -> value)"
          ),
          Seq(
            "200 - Edges"
          )
        ),pathBuilder(
          "GET - /edges/{id}",
          "Get an edge by ID",
          Seq(
            "Path(\"id\") - The ID of the edge"
          ),
          Seq(
            "200 - Model: Edge",
            "404 - Edge could not be found"
          )
        ),
        pathBuilder(
          "PUT - /edges/{id}",
          "Update an edge by ID",
          Seq(
            "Path(\"id\") - The ID of the edge",
            "Body - Object of data to upsert into the edge"
          ),
          Seq(
            "200 - Model: Edge",
            "404 - Edge could not be found"
          )
        ),
        pathBuilder(
          "DELETE - /edges/{id}",
          "Delete an edge by ID",
          Seq(
            "Path(\"id\") - The ID of the edge"
          ),
          Seq(
            "204 - (Empty Body)",
            "404 - Edge could not be found"
          )
        ),
        pathBuilder(
          "GET - /edges",
          "Fetch a stream of edges by optional label and optional data",
          Seq(
            "Query(\"label\") - Optional label filter",
            "Body - Optional object of node data filters (key -> value)"
          ),
          Seq(
            "200 - Model: Edges"
          )
        ),
        pathBuilder(
          "POST - /nodes/{_1}/to/{_2}",
          "Create an edge from _1 to _2",
          Seq(
            "Path(\"_1\") - ID of the head/source node",
            "Path(\"_2\") - ID of the last/destination node",
            "Query(\"label\") - Required edge label",
            "Body - Object of data for the edge"
          ),
          Seq(
            "200 - Model: Edge",
            "400 - (_1 or _2 could not be found)"
          )
        )
      ).mkString("\n")

    val models =
      """<h3>Models</h3>
        |
        |<h4>Node</h4>
        |<pre>
        |{
        |  "id": String,
        |  "label": String,
        |  "data": Json Object
        |}
        |</pre>
        |
        |<h4>Edge</h4>
        |<pre>
        |{
        |  "id": String,
        |  "label": String,
        |  "_1": String/ID,
        |  "_2": String/ID,
        |  "data": Json Object
        |}
        |</pre>
        |
        |<h4>Nodes</h4>
        |Chunked Bytestring, where each Chunk is represented by the Node model
        |
        |<h4>Edges</h4>
        |Chunked Bytestring, where each Chunk is represented by the Edge model""".stripMargin

    Seq(
      header,
      routes,
      models
    ).mkString("\n")
  }
}

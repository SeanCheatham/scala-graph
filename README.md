Graph library/interface with adapter(s), written in Scala

# Overview
This library serves two purposes:
1. Provide a common abstraction for accessing and manipulating a graph data structure
2. Provide adapters for various databases (particularly graph databases)

I am a one-man show, so at best, what you see here is work I need in side projects.  I've open-sourced this library because other people may find some of it useful.

My current focus is on providing an abstraction for the *Neo4j* graph database.  As such, I have provided a common interface for accessing either an embedded database, or a remote/production instance.

# Usage
This library is written in Scala.  It _might_ interoperate with other JVM languages, but I make no guarantees.

## Include the library in your project.
## Create a Graph instance
### Create a mutable in-memory graph:
```scala
import com.seancheatham.graph.adapters.memory.MutableGraph
val graph =
    new MutableGraph
```
### Create an embedded Neo4jGraph:
```scala
// Create a temporary Neo4j graph
import com.seancheatham.graph.adapters.neo4j._
val graph = 
    Neo4jGraph.embedded()
    
// Create a graph which persists to disk
import com.seancheatham.graph.adapters.neo4j._
val graph = 
    Neo4jGraph.embedded("/path/to/save/to")
```

### Connect to a remote Neo4j Instance
```scala
import com.seancheatham.graph.adapters.neo4j._

val address = 
    "bolt://192.168.?.?"
    
    
// If auth is required
import org.neo4j.driver.v1.AuthTokens

val auth = 
    AuthTokens.basic("username", "password")
    
val graph = 
    Neo4jGraph(address, auth)
    
    
// If auth is not required
val graph =
    Neo4jGraph(address)
```

## Create a node
```scala
import play.api.libs.json._

val node1: Node = 
    graph.addNode("label", Map("name" -> JsString("potato")))
```
## Get a node by ID
```scala
val alsoNode1: Option[Node] = 
    graph.getNode("1")
```
## Get nodes by label and/or data
```scala
val nodes: TraversableOnce[Node] = 
    graph.getNodes(Some("label"), Map("name" -> JsString("potato")))
```
## Create an edge between two nodes
```scala
val edge1: Edge =
    graph.addEdge(node1, node2, "edge_label", Map("weight" -> Json.toJson(1.5)))

// Or you can use some syntactic sugar:
import com.seancheatham.graph.Edge.NodeEdgeSyntax
val edge1: Edge =
    graph.addEdge(node1 -"LABEL"-> node2, Map("weight" -> Json.toJson(1.5)))
```
## Fetch inbound or outbound edges for a node
```scala
val incomingEdges: TraversableOnce[Edge] =
    graph.getIngressEdges(node1)
    
val outgoingEdges: TraversableOnce[Edge] =
    graph.getEgressEdges(node1)
```
## Update a node/edge
```scala
val updatedNode1 =
    graph.updateNode(node1)("name" -> JsString("carrot"), "category" -> JsString("vegetable"))
    
val updatedEdge1 =
    graph.updateEdge(edge1)("weight" -> Json.toJson(2.3))
```

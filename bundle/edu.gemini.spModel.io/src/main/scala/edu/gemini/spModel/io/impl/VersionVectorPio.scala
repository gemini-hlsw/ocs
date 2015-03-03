package edu.gemini.spModel.io.impl

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.sp.version._
import edu.gemini.shared.util._
import edu.gemini.spModel.pio._

import scala.collection.JavaConverters._

/**
 * Utility for converting from a version vector map to a Pio Container and
 * vice versa.
 */
object VersionVectorPio {
  val kind = "versions"

  def toContainer(f: PioFactory, vm: VersionMap): Container = {
    val c = f.createContainer(kind, kind, "1.0")

    vm foreach {
      // Every map entry corresponds to a node and its versions.  So make a
      // paramset per map entry / node.
      case (key, vv) =>
        val node = f.createParamSet("node")
        c.addParamSet(node)

        // Add the key to identify the node.
        Pio.addParam(f, node, "key", key.toString)

        // Add each database's version of that node.
        vv.clocks foreach {
          case (id, version) => Pio.addIntParam(f, node, id.toString, version)
        }
    }

    c
  }

  def toVersions(c: Container): VersionMap = {
    // Get all the container's "node" ParamSet children as a List[Node]
    val nodes = c.getParamSets("node").asScala.toList.asInstanceOf[List[ParamSet]] map { Node(_) }

    // Get a map String -> LifespanId so we can avoid recreating equivalent LifespanIds
    val ids = (Map.empty[String,LifespanId]/:nodes) { (m, node) =>
      (m/:node.versions.unzip._1) { (idMap, idStr) =>
        if (idMap.contains(idStr)) idMap else idMap + (idStr -> LifespanId.fromString(idStr))
      }
    }

    // Fold and empty version map over the Node, converting each Node into a
    // SPNodeKey -> DbVersions tuple to add it to the version map.
    (EmptyVersionMap/:nodes) { (vm, node) => vm + node.toTuple(ids) }.toMap
  }

  private object Node {
    def apply(pset: ParamSet): Node = {
      // Separate the one "key" param from the zero or more db version params
      // for this node.
      val params = pset.getParams.asScala.toList.asInstanceOf[List[Param]]
      val (keyParams, versionParams) = params.span(_.getName.equals("key"))

      val nodeKey      = new SPNodeKey(keyParams.head.getValue)
      val nodeVersions = versionParams map { p =>
        (p.getName, java.lang.Integer.valueOf(p.getValue))
      }
      Node(nodeKey, nodeVersions)
    }
  }

  private case class Node(key: SPNodeKey, versions: List[(String, java.lang.Integer)]) {
    def toTuple(ids: Map[String, LifespanId]): (SPNodeKey, NodeVersions) =
      key -> VersionVector(versions map {
        case (idStr, intVal) => ids(idStr) -> intVal
      }: _*)
  }
}

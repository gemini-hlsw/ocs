package edu.gemini.sp.vcs

import edu.gemini.pot.sp.{ISPProgram, ISPNode, SPNodeKey}
import edu.gemini.pot.sp.version._
import edu.gemini.sp.vcs.Status._

import scalaz._
import Scalaz._

case class NodeStatus(status: Status, upToDate: Boolean) {
  override def toString = "%s%s".format(status.abbr, if (upToDate) " " else "*")
}

case class NodeData(key: SPNodeKey, node: Option[ISPNode], vv: NodeVersions) {
  def status(thatVv: NodeVersions): NodeStatus = {
    val comp = vv.tryCompareTo(thatVv)
    val stat = if (comp.exists(_ <= 0))
      Unchanged
    else if (node.isEmpty)
      Deleted
    else if (thatVv.isEmpty)
      Added
    else
      Modified

    NodeStatus(stat, comp.exists(_ >= 0))
  }
}

object NodeData {
  type NodeDataMap = Map[SPNodeKey, NodeData]

  def map(p: ISPProgram): NodeDataMap = {
    val nm = nodeMap(p)
    (p.getVersions map {
      case (key, vv) => key -> NodeData(key, nm.get(key), vv)
    }).toMap
  }

  type StatusMap = Map[SPNodeKey, NodeStatus]

  def statusMap(p: ISPProgram, thatVm: VersionMap): StatusMap =
    map(p) mapValues { nd => nd.status(nodeVersions(thatVm, nd.key)) }
}
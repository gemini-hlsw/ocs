package edu.gemini.sp.vcs.tui.osgi

import edu.gemini.pot.sp._
import edu.gemini.sp.vcs.NodeData.StatusMap

import edu.gemini.spModel.data.AbstractDataObject

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/**
 * Formatting of science program nodes and their status.
 */
object StatusFormat {
  def apply(tup: (ISPProgram, StatusMap)): String = {
    val (prog, m) = tup
    formatStatus(Nil, 1, prog, m).mkString("\n")
  }

  private def formatStatus(in: List[String], indent: Int, node: ISPNode, statusMap: StatusMap): List[String] = {
    val children = node match {
      case tg: ISPTemplateGroup => tg.getChildren.asScala.toList.filter(!_.isInstanceOf[ISPTemplateParameters])
      case c: ISPContainerNode  => c.getChildren.asScala.toList
      case _                    => Nil
    }
    val formattedChildren = (children:\in) { (node, lst) =>
      formatStatus(lst, indent+2, node, statusMap)
    }
    "%s%s%s".format(statusMap(node.getNodeKey), " " * indent, formatNode(node)) :: formattedChildren
  }

  private def formatNode(node: ISPNode): String = {
    val typeString = node match {
      case prog: ISPProgram      => Option(prog.getProgramID).map(_.toString).getOrElse("Program")
      case obs: ISPObservation   => obs.getObservationIDAsString("Observation")
      case oc: ISPObsComponent   => formatSpType(oc.getType)
      case sc: ISPSeqComponent   => formatSpType(sc.getType)
      case tf: ISPTemplateFolder => "Templates"
      case tg: ISPTemplateGroup  => "Template Group"
      case _ => ""
    }

    val title = ~(for {
      d <- Option(node.getDataObject)
      t <- Option(d.asInstanceOf[AbstractDataObject].getTitle)
    } yield t)

    "%s: %s".format(typeString, title)
  }

  private def formatSpType(spType: SPComponentType): String =
    "%s (%s)".format(spType.broadType, spType.narrowType)
}

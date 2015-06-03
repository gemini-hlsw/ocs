package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Graces {
  def apply() = new FiberNode

  class FiberNode extends SingleSelectNode[Unit, GracesFiberMode, GracesFiberMode](()) {
    val title = "Fiber Mode"
    val description = "Select a fiber mode for your configuration."

    def choices: List[GracesFiberMode] = GracesFiberMode.values.toList

    def apply(fm: GracesFiberMode) = Left(new ReadNode(fm))

    override def default = Some(GracesFiberMode.forName("ONE_FIBER"))

    def unapply = {
      case b: GracesBlueprint => b.fiberMode
    }
  }

  class ReadNode(fm: GracesFiberMode)  extends SingleSelectNode[GracesFiberMode, GracesReadMode, GracesBlueprint](fm) {
    def title = "Read Mode"
    def description = "Select a read mode for your configuration."

    def apply(rm: GracesReadMode) = Right(GracesBlueprint(fm, rm))

    override def default = Some(GracesReadMode.forName("NORMAL"))

    def unapply = {
      case b: GracesBlueprint => b.readMode
    }

    def choices = GracesReadMode.values.toList
  }

}
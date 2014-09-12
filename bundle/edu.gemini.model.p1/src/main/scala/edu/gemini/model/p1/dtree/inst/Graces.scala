package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._
import scala.Some

object Graces {
  def apply() = new FiberNode

  class FiberNode extends SingleSelectNode[Unit, GracesFiberMode, GracesBlueprint](()) {
    val title = "Fiber Mode"
    val description = "Select a fiber mode for your configuration."

    def choices: List[GracesFiberMode] = GracesFiberMode.values.toList

    def apply(fm: GracesFiberMode) = Right(GracesBlueprint(fm))

    override def default = Some(GracesFiberMode.forName("ONE_FIBER"))

    def unapply = {
      case b: GracesBlueprint => b.fiberMode
    }
  }

}
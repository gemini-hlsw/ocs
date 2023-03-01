package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Ghost {
  def apply() = new ResolutionModeNode

  class ResolutionModeNode extends SingleSelectNode[Unit, GhostResolutionMode, GhostResolutionMode](()) {
    val title = "Resolution Mode"
    val description = "Select a resolution mode for your configuration."

    def choices: List[GhostResolutionMode] = GhostResolutionMode.values.filterNot(_ == GhostResolutionMode.PrecisionRadialVelocity).toList

    def apply(om: GhostResolutionMode) = Left(new TargetModeNode(om))

    override def default = Some(GhostResolutionMode.forName("STANDARD"))

    def unapply = {
      case b: GhostBlueprint => b.resolutionMode
    }
  }

  class TargetModeNode(rm: GhostResolutionMode) extends SingleSelectNode[GhostResolutionMode, GhostTargetMode, GhostBlueprint](rm){
    def title = "Target Mode"

    def description = "Select a target mode for your configuration."

    def apply(d: GhostTargetMode) = Right(GhostBlueprint(rm, d))

    override def default = Some(GhostTargetMode.forName("SINGLE"))

    def unapply = {
      case b: GhostBlueprint => b.targetMode
    }

    def choices = if (rm == GhostResolutionMode.Standard) GhostTargetMode.values.toList else List(GhostTargetMode.Single)
  }

}

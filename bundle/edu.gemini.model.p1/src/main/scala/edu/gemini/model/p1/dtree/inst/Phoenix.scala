package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Phoenix {
  def apply() = new FocalPlaneUnitNode

  class FocalPlaneUnitNode extends SingleSelectNode[Unit, PhoenixFocalPlaneUnit, PhoenixFocalPlaneUnit](()) {
    val title = "Focal Plane Unit"
    val description = "Select a focal plane unit for your configuration."

    def choices: List[PhoenixFocalPlaneUnit] = PhoenixFocalPlaneUnit.values.toList

    def apply(om: PhoenixFocalPlaneUnit) = Left(new FilterNode(om))

    override def default = Some(PhoenixFocalPlaneUnit.forName("MASK_3"))

    def unapply = {
      case b: PhoenixBlueprint => b.fpu
    }
  }

  class FilterNode(fpu: PhoenixFocalPlaneUnit) extends SingleSelectNode[PhoenixFocalPlaneUnit, PhoenixFilter, PhoenixBlueprint](fpu){
    def title = "Filter"
    def description = "Select a filter for your configuration."

    def apply(f: PhoenixFilter) = Right(PhoenixBlueprint(fpu, f))

    override def default = Some(PhoenixFilter.forName("K4396"))

    def unapply = {
      case b: PhoenixBlueprint => b.filter
    }

    def choices = PhoenixFilter.values.toList
  }

}
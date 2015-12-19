package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Phoenix {
  def apply() = new SiteNode

  class SiteNode extends SingleSelectNode[Unit, VisitorSite, Site](()) {
    val title       = "Site"
    val description = "Select the site."
    def choices     = GNVisitorSite :: GSVisitorSite :: Nil

    def apply(s: VisitorSite) = Left(new FocalPlaneUnitNode(s.site))

    def unapply = {
      case b: PhoenixBlueprint => b.site
    }
  }


  class FocalPlaneUnitNode(s: Site) extends SingleSelectNode[Site, PhoenixFocalPlaneUnit, PhoenixFocalPlaneUnit](s) {
    val title = "Focal Plane Unit"
    val description = "Select a focal plane unit for your configuration."

    def choices: List[PhoenixFocalPlaneUnit] = PhoenixFocalPlaneUnit.values.toList

    def apply(om: PhoenixFocalPlaneUnit) = Left(new FilterNode(s, om))

    override def default = Some(PhoenixFocalPlaneUnit.forName("MASK_3"))

    def unapply = {
      case b: PhoenixBlueprint => b.fpu
    }
  }

  class FilterNode(s: Site, fpu: PhoenixFocalPlaneUnit) extends SingleSelectNode[PhoenixFocalPlaneUnit, PhoenixFilter, PhoenixBlueprint](fpu){
    def title = "Filter"
    def description = "Select a filter for your configuration."

    def apply(f: PhoenixFilter) = Right(PhoenixBlueprint(s, fpu, f))

    override def default = Some(PhoenixFilter.forName("K4396"))

    def unapply = {
      case b: PhoenixBlueprint => b.filter
    }

    def choices = PhoenixFilter.values.toList
  }

}
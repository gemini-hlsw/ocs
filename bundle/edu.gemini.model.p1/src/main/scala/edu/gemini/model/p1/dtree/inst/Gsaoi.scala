package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Gsaoi {
  def apply() = new FilterNode

  class FilterNode extends MultiSelectNode[Unit, GsaoiFilter, GsaoiBlueprint](()) {
    val title = "Filters"
    val description = "Select a filter for your configuration."

    def choices: List[GsaoiFilter] = GsaoiFilter.values.toList

    def apply(fs: List[GsaoiFilter]) = Right(GsaoiBlueprint(fs))

    def unapply = {
      case b: GsaoiBlueprint => b.filters
    }
  }

}
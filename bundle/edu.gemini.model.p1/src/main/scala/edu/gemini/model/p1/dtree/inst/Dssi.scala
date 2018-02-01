package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Dssi {

  def apply() = new SiteNode

  class SiteNode extends SingleSelectNode[Unit, VisitorSite, DssiBlueprint](()) {
    val title       = "Site"
    val description = "Select the site."
    def choices     = GSVisitorSite :: Nil

    def apply(fs: VisitorSite) = Right(DssiBlueprint(fs))

    def unapply = {
      case b: DssiBlueprint => b.site
    }
  }

}
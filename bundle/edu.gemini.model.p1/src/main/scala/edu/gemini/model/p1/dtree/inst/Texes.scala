package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Texes {

  def apply() = new SiteNode

  class SiteNode extends SingleSelectNode[Unit, VisitorSite, Site](()) {
    val title       = "Site"
    val description = "Select the site."
    def choices     = GNVisitorSite :: Nil

    def apply(s: VisitorSite) = Left(new DisperserNode(s.site))

    def unapply = {
      case b: PhoenixBlueprint => b.site
    }
  }

  class DisperserNode(s: Site) extends SingleSelectNode[Site, TexesDisperser, TexesBlueprint](s) {
    val title       = "Disperser"
    val description = "Select the disperser to use."
    def choices     = TexesDisperser.values.toList

    def apply(ds: TexesDisperser) = Right(new TexesBlueprint(s, ds))

    def unapply = {
      case b: TexesBlueprint => b.disperser
    }
  }

}
package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

// Intermediate object used to get a nice looking Site selection box
sealed trait VisitorSite {
  val site: Site
  override def toString = site.name
}
case object GSVisitorSite extends VisitorSite {
  override val site: Site = Site.GS
}
case object GNVisitorSite extends VisitorSite {
  override val site: Site = Site.GN
}

object VisitorSite {
  // Implicit conversions
  implicit val toSite = (s: VisitorSite) => s.site

  implicit val fromSite = (s: Site) => s match {
    case Site.GS => GSVisitorSite
    case Site.GN => GNVisitorSite
    case _       => sys.error("Visitors can only belong to GS or GN")
  }
}

object Visitor {

  def apply() = new SiteNode

  class SiteNode extends SingleSelectNode[Unit, VisitorSite, VisitorSite](()) {
    val title       = "Site"
    val description = "Select the site."
    def choices     = GNVisitorSite :: GSVisitorSite :: Nil

    def apply(fs: VisitorSite) = Left(new CustomNameNode(fs))

    def unapply = {
      case b: VisitorBlueprint => b.site
    }
  }

  class CustomNameNode(fs: VisitorSite) extends TextNode[VisitorSite, VisitorBlueprint](fs) {
    val title       = "Instrument name"
    val description = "Enter the name of the instrument."

    def apply(n: String) = Right(VisitorBlueprint(fs, n))

    def unapply = {
      case b: VisitorBlueprint => b.customName
    }

  }

}
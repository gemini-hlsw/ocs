package edu.gemini.catalog.api

import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.core.Target.SiderealTarget

case class CatalogQuery(base: Coordinates, radiusConstraint: RadiusConstraint, catalog: CatalogName = ucac4) {
  def filter: SiderealTarget => Boolean = (t) => radiusConstraint.targetsFilter(base)(t)
}

sealed abstract class CatalogName(val id: String)

case object sdss extends CatalogName("sdss9")
case object gsc234 extends CatalogName("gsc234")
case object ppmxl extends CatalogName("ppmxl")
case object ucac4 extends CatalogName("ucac4")
case object twomass_psc extends CatalogName("twomass_psc")
case object twomass_xsc extends CatalogName("twomass_xsc")

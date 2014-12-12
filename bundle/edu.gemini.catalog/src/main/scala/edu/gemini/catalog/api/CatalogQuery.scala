package edu.gemini.catalog.api

import edu.gemini.spModel.core.{Angle, Coordinates}

case class CatalogQuery(base: Coordinates, coneRadius: Angle, catalog: CatalogName = sdss)

sealed abstract class CatalogName(val id: String)

case object sdss extends CatalogName("sdss9")
case object gsc234 extends CatalogName("gsc234")
case object ppmxl extends CatalogName("ppmxl")
case object ucac4 extends CatalogName("ucac4")
case object twomass_psc extends CatalogName("twomass_psc")
case object twomass_xsc extends CatalogName("twomass_xsc")
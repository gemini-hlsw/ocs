package edu.gemini.model.p1.immutable

sealed abstract class Instrument(val site: Site, val id: String, val display: String, val gsa: String) {
  def this(site: Site, id: String) = this(site, id, id, id)
  override def toString = display
}

import Site._

object Instrument {
  case object GmosNorth  extends Instrument(GN, "GMOS", "GMOS North", "GMOS-N")
  case object Gnirs      extends Instrument(GN, "GNIRS")
  case object Michelle   extends Instrument(GN, "Michelle", "Michelle", "michelle")
  case object Nifs       extends Instrument(GN, "NIFS")
  case object Niri       extends Instrument(GN, "NIRI")
  case object Dssi       extends Instrument(GN, "DSSI", "DSSI", "DSSI")
  case object Texes      extends Instrument(GN, "Texes", "Texes", "TEXES")
  case object ʻAlopeke   extends Instrument(GN, "ʻAlopeke")

  case object Flamingos2 extends Instrument(GS, "Flamingos2", "Flamingos2", "FLAMINGOS")
  case object GmosSouth  extends Instrument(GS, "GMOSSouth", "GMOS South", "GMOS-S")
  case object Gpi        extends Instrument(GS, "GPI")
  case object Graces     extends Instrument(GN, "GRACES")
  case object Gsaoi      extends Instrument(GS, "GSAOI")
  case object Nici       extends Instrument(GS, "NICI")
  case object Phoenix    extends Instrument(GS, "Phoenix", "Phoenix", "PHOENIX")
  case object Trecs      extends Instrument(GS, "TReCS", "T-ReCS", "TReCS")
  case object Visitor    extends Instrument(GS, "Visitor")

//  case object Keck extends Instrument(Site.Keck, "Keck")
//  case object Subaru extends Instrument(Site.Subaru, "Subaru")
}

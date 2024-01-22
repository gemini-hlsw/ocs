package edu.gemini.model.p1.immutable

sealed abstract class Instrument(val site: Site, val id: String, val display: String, val gsa: String) extends Product with Serializable {
  def this(site: Site, id: String) = this(site, id, id, id)
  override def toString: String = display
}

import Site._

object Instrument {
  case object GmosNorth  extends Instrument(GN, "GMOS", "GMOS North", "GMOS-N")
  case object Gnirs      extends Instrument(GN, "GNIRS")
  case object Michelle   extends Instrument(GN, "Michelle", "Michelle", "michelle")
  case object Nifs       extends Instrument(GN, "NIFS")
  case object Niri       extends Instrument(GN, "NIRI")
  case object Dssi       extends Instrument(GN, "DSSI")
  case object Texes      extends Instrument(GN, "Texes", "Texes", "TEXES")
  case object Alopeke    extends Instrument(GN, "Alopeke", "'Alopeke", "Alopeke")
  case object Igrins2    extends Instrument(GN, "IGRINS-2", "IGRINS-2", "IGRINS-2")

  case object Flamingos2 extends Instrument(GS, "Flamingos2", "Flamingos2", "FLAMINGOS")
  case object Ghost      extends Instrument(GS, "GHOST")
  case object GmosSouth  extends Instrument(GS, "GMOSSouth", "GMOS South", "GMOS-S")
  case object Gpi        extends Instrument(GS, "GPI")
  case object Graces     extends Instrument(GN, "GRACES")
  case object Gsaoi      extends Instrument(GS, "GSAOI")
  case object Igrins     extends Instrument(GS, "IGRINS")
  case object Nici       extends Instrument(GS, "NICI")
  case object Phoenix    extends Instrument(GS, "Phoenix", "Phoenix", "PHOENIX")
  case object Trecs      extends Instrument(GS, "TReCS", "T-ReCS", "TReCS")
  case object Visitor    extends Instrument(GS, "Visitor")
  case object Zorro      extends Instrument(GS, "Zorro")
  case object MaroonX    extends Instrument(GN, "MAROON-X")

  def fromMutable(id: String): Instrument = id match {
    case "GMOS"       => GmosNorth
    case "GNIRS"      => Gnirs
    case "Michelle"   => Michelle
    case "NIFS"       => Nifs
    case "NIRI"       => Niri
    case "DSSI"       => Dssi
    case "Texes"      => Texes
    case "Alopeke"    => Alopeke
    case "Igrins2"    => Igrins2

    case "Flamingos2" => Flamingos2
    case "GHOST"      => Ghost
    case "GMOSSouth"  => GmosSouth
    case "GPI"        => Gpi
    case "GRACES"     => Graces
    case "GSAOI"      => Gsaoi
    case "IGRINS"     => Igrins
    case "NICI"       => Nici
    case "Phoenix"    => Phoenix
    case "TReCS"      => Trecs
    case "Visitor"    => Visitor
    case "Zorro"      => Zorro
    case "MAROON-X"   => MaroonX
    case _            => sys.error("Unsupported instrument")
  }

}

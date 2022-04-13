package edu.gemini.spModel.core

import edu.gemini.spModel.core.WavelengthConversions._

sealed abstract class MagnitudeBand private (val name: String, val start: Wavelength, val center: Wavelength, val end: Wavelength, val description: Option[String], val defaultSystem: MagnitudeSystem) extends Product with Serializable {

  private def this(name: String, start: Wavelength, center: Wavelength, end: Wavelength) =
    this(name, start, center, end, None, MagnitudeSystem.Vega)

  private def this(name: String, start: Wavelength, center: Wavelength, end: Wavelength, description: String) =
    this(name, start, center, end, Some(description), MagnitudeSystem.Vega)

  private def this(name: String, start: Wavelength, center: Wavelength, end: Wavelength, description: String, defaultMagnitudeSystem: MagnitudeSystem) =
    this(name, start, center, end, Some(description), defaultMagnitudeSystem)

  final override def toString = name

}

object MagnitudeBand {
  // OCSADV-203
  // Class files clobber one another on OS X so names can't differ only in case.
  // We may need to revisit and come up with better names.
  // Values for Sloan filters (u', g', r', i', z') taken from Fukugita et al. (1996)
  case object _u extends MagnitudeBand("u",   333.nm,  356.nm,  379.nm, "UV",             MagnitudeSystem.AB)
  case object _g extends MagnitudeBand("g",   433.nm,  483.nm,  533.nm, "green",          MagnitudeSystem.AB)
  case object _r extends MagnitudeBand("r",   578.nm,  626.nm,  674.nm, "red",            MagnitudeSystem.AB)
  case object _i extends MagnitudeBand("i",   714.nm,  767.nm,  820.nm, "far red",        MagnitudeSystem.AB)
  case object _z extends MagnitudeBand("z",   847.nm,  910.nm,  973.nm, "near-infrared",  MagnitudeSystem.AB)

  case object U  extends MagnitudeBand("U",   322.nm,   360.nm,   398.nm, "ultraviolet")
  case object B  extends MagnitudeBand("B",   395.nm,   440.nm,   485.nm, "blue")
  case object V  extends MagnitudeBand("V",   507.nm,   550.nm,   593.nm, "visual")
  case object UC extends MagnitudeBand("UC",  578.nm,   610.nm,   642.nm, "UCAC")
  case object R  extends MagnitudeBand("R",   620.nm,   670.nm,   720.nm, "red")
  case object I  extends MagnitudeBand("I",   820.nm,   870.nm,   920.nm, "infrared")
  case object Y  extends MagnitudeBand("Y",   960.nm,  1020.nm,  1080.nm)
  case object J  extends MagnitudeBand("J",  1130.nm,  1250.nm,  1370.nm)
  case object H  extends MagnitudeBand("H",  1500.nm,  1650.nm,  1800.nm)
  case object K  extends MagnitudeBand("K",  1995.nm,  2200.nm,  2405.nm)
  case object L  extends MagnitudeBand("L",  3410.nm,  3760.nm,  4110.nm)
  case object M  extends MagnitudeBand("M",  4650.nm,  4770.nm,  4890.nm)
  case object N  extends MagnitudeBand("N",  7855.nm, 10470.nm, 13085.nm)
  case object Q  extends MagnitudeBand("Q", 19305.nm, 20130.nm, 20955.nm)

  // Using 1% transmission to characterize "start" and "end"
  case object GB extends MagnitudeBand("G_BP", 328.nm,  513.nm,  671.nm, "Gaia Blue Passband")
  case object G  extends MagnitudeBand("G",    330.nm,  641.nm, 1037.nm, "Gaia Passband")
  case object GR extends MagnitudeBand("G_RP", 626.nm,  778.nm, 1051.nm, "Gaia Red Passband")

  // use V-Band center and width for apparent magnitudes
  case object AP extends MagnitudeBand("AP", V.start, V.center, V.end, Some("apparent"), MagnitudeSystem.Vega)

  lazy val all: List[MagnitudeBand] =
    List(_u, _g, _r, _i, _z, U, B, V, UC, R, I, Y, J, H, K, L, M, N, Q, AP, G, GB, GR)

  def fromString(s: String): Option[MagnitudeBand] =
    all.find(_.name == s)

  def unsafeFromString(s: String): MagnitudeBand =
    fromString(s).getOrElse(sys.error("Unknown magnitude band: " + s))

  // order by central wavelength; make sure that AP always shows up last because it's sort of a special case
  implicit val MagnitudeBandOrder: scalaz.Order[MagnitudeBand] =
    scalaz.Order.orderBy {
      case AP   => Double.MaxValue.nm
      case band => band.center
    }

  implicit val MagnitudeBandOrdering: scala.math.Ordering[MagnitudeBand] =
    MagnitudeBandOrder.toScalaOrdering

  /** @group Typeclass Instances */
  implicit val equals = scalaz.Equal.equalA[MagnitudeBand]

}

package edu.gemini.spModel.core

import edu.gemini.spModel.core.WavelengthConversions._

sealed abstract class MagnitudeBand private (val name: String, val center: Wavelength, val width: Wavelength, val description: Option[String], val defaultSystem: MagnitudeSystem) extends Product with Serializable {

  private def this(name: String, center: Wavelength, width: Wavelength) =
    this(name, center, width, None, MagnitudeSystem.Vega)

  private def this(name: String, center: Wavelength, width: Wavelength, description: String) =
    this(name, center, width, Some(description), MagnitudeSystem.Vega)

  private def this(name: String, center: Wavelength, width: Wavelength, description: String, defaultMagnitudeSystem: MagnitudeSystem) =
    this(name, center, width, Some(description), defaultMagnitudeSystem)

  val start: Wavelength = center - width / 2

  val end:   Wavelength = center + width / 2

  final override def toString = name

}

object MagnitudeBand {
  // OCSADV-203
  // Class files clobber one another on OS X so names can't differ only in case.
  // We may need to revisit and come up with better names.
  // Values for Sloan filters (u', g', r', i', z') taken from Fukugita et al. (1996)
  case object _u extends MagnitudeBand("u",   356.nm,   46.nm, "UV",             MagnitudeSystem.AB)
  case object _g extends MagnitudeBand("g",   483.nm,   99.nm, "green",          MagnitudeSystem.AB)
  case object _r extends MagnitudeBand("r",   626.nm,   96.nm, "red",            MagnitudeSystem.AB)
  case object _i extends MagnitudeBand("i",   767.nm,  106.nm, "far red",        MagnitudeSystem.AB)
  case object _z extends MagnitudeBand("z",   910.nm,  125.nm, "near-infrared",  MagnitudeSystem.AB)

  case object U  extends MagnitudeBand("U",   360.nm,   75.nm, "ultraviolet")
  case object B  extends MagnitudeBand("B",   440.nm,   90.nm, "blue")
  case object V  extends MagnitudeBand("V",   550.nm,   85.nm, "visual")
  case object UC extends MagnitudeBand("UC",  610.nm,   63.nm, "UCAC")
  case object R  extends MagnitudeBand("R",   670.nm,  100.nm, "red")
  case object I  extends MagnitudeBand("I",   870.nm,  100.nm, "infrared")
  case object Y  extends MagnitudeBand("Y",  1020.nm,  120.nm)
  case object J  extends MagnitudeBand("J",  1250.nm,  240.nm)
  case object H  extends MagnitudeBand("H",  1650.nm,  300.nm)
  case object K  extends MagnitudeBand("K",  2200.nm,  410.nm)
  case object L  extends MagnitudeBand("L",  3760.nm,  700.nm)
  case object M  extends MagnitudeBand("M",  4770.nm,  240.nm)
  case object N  extends MagnitudeBand("N", 10470.nm, 5230.nm)
  case object Q  extends MagnitudeBand("Q", 20130.nm, 1650.nm)

  // use V-Band center and width for apparent magnitudes
  case object AP extends MagnitudeBand("AP", V.center, V.width, Some("apparent"), MagnitudeSystem.Vega)

  lazy val all: List[MagnitudeBand] =
    List(_u, _g, _r, _i, _z, U, B, V, UC, R, I, Y, J, H, K, L, M, N, Q, AP)

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


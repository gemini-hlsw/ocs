package edu.gemini.spModel.core

sealed abstract class MagnitudeBand private (val name: String, val center: Wavelength, val width: Wavelength, val description: Option[String], val defaultSystem: MagnitudeSystem) extends Product with Serializable {

  private def this(name: String, center: Wavelength, width: Wavelength) =
    this(name, center, width, None, MagnitudeSystem.VEGA)

  private def this(name: String, center: Wavelength, width: Wavelength, description: String) =
    this(name, center, width, Some(description), MagnitudeSystem.VEGA)

  private def this(name: String, center: Wavelength, width: Wavelength, description: String, defaultMagnitudeSystem: MagnitudeSystem) =
    this(name, center, width, Some(description), defaultMagnitudeSystem)

  val start: Wavelength = center + width / 2

  val end:   Wavelength = center - width / 2

}

object MagnitudeBand {
  // Nicer access to R-Band for Java Clients
  def rBand = R

  // OCSADV-203
  // Class files clobber one another on OS X so names can't differ only in case.
  // We may need to revisit and come up with better names.
  // Values for Sloan filters (u', g', r', i', z') taken from Fukugita et al. (1996)
  case object _u extends MagnitudeBand("u", nm(  356), nm(  46), "UV",             MagnitudeSystem.AB)
  case object _g extends MagnitudeBand("g", nm(  483), nm(  99), "green",          MagnitudeSystem.AB)
  case object _r extends MagnitudeBand("r", nm(  626), nm(  96), "red",            MagnitudeSystem.AB)
  case object _i extends MagnitudeBand("i", nm(  767), nm( 106), "far red",        MagnitudeSystem.AB)
  case object _z extends MagnitudeBand("z", nm(  910), nm( 125), "near-infrared",  MagnitudeSystem.AB)

  case object U  extends MagnitudeBand("U", nm(  360), nm(  75), "ultraviolet")
  case object B  extends MagnitudeBand("B", nm(  440), nm(  90), "blue")
  case object V  extends MagnitudeBand("V", nm(  550), nm(  85), "visual")
  case object UC extends MagnitudeBand("UC",nm(  610), nm(  63), "UCAC")
  case object R  extends MagnitudeBand("R", nm(  670), nm( 100), "red")
  case object I  extends MagnitudeBand("I", nm(  870), nm( 100), "infrared")
  case object Y  extends MagnitudeBand("Y", nm( 1020), nm( 120))
  case object J  extends MagnitudeBand("J", nm( 1250), nm( 240))
  case object H  extends MagnitudeBand("H", nm( 1650), nm( 300))
  case object K  extends MagnitudeBand("K", nm( 2200), nm( 410))
  case object L  extends MagnitudeBand("L", nm( 3760), nm( 700))
  case object M  extends MagnitudeBand("M", nm( 4770), nm( 240))
  case object N  extends MagnitudeBand("N", nm(10470), nm(5230))
  case object Q  extends MagnitudeBand("Q", nm(20130), nm(1650))

  // use V-Band center and width for apparent magnitudes
  case object AP extends MagnitudeBand("AP", V.center, V.width, Some("apparent"), MagnitudeSystem.VEGA)

  lazy val all: List[MagnitudeBand] =
    List(_u, _g, _r, _i, _z, U, B, V, UC, R, I, Y, J, H, K, L, M, N, Q, AP)

  // order by central wavelength; make sure that AP always shows up last because it's sort of a special case
  implicit val MagnitudeBandOrder: scalaz.Order[MagnitudeBand] =
    scalaz.Order.orderBy {
      case AP   => Wavelength.fromNanometers(Double.MaxValue)
      case band => band.center
    }

  implicit val MagnitudeBandOrdering: scala.math.Ordering[MagnitudeBand] =
    MagnitudeBandOrder.toScalaOrdering

  /** @group Typeclass Instances */
  implicit val equals = scalaz.Equal.equalA[MagnitudeBand]

  // helper to create nanometers
  private def nm(d: Double) = Wavelength.fromNanometers(d)

}


package edu.gemini.spModel.core

sealed abstract class MagnitudeBand private (val name: String, val wavelengthMidPointNm: Option[Int], val description: Option[String], val defaultSystem: MagnitudeSystem) extends Product with Serializable {

  private def this(name: String, wavelengthMidPointNm: Int) =
    this(name, Some(wavelengthMidPointNm), None, MagnitudeSystem.VEGA)

  private def this(name: String, wavelengthMidPointNm: Int, description: String) =
    this(name, Some(wavelengthMidPointNm), Some(description), MagnitudeSystem.VEGA)

  private def this(name: String, wavelengthMidPointNm: Int, description: String, defaultMagnitudeSystem: MagnitudeSystem) =
    this(name, Some(wavelengthMidPointNm), Some(description), defaultMagnitudeSystem)

}

object MagnitudeBand {

  // OCSADV-203
  // Class files clobber one another on OS X so names can't differ only in case.
  // We may need to revisit and come up with better names.
  case object _u extends MagnitudeBand("u", 350, "UV", MagnitudeSystem.AB)
  case object _g extends MagnitudeBand("g", 475, "green", MagnitudeSystem.AB)
  case object _r extends MagnitudeBand("r", 630, "red", MagnitudeSystem.AB)
  case object _i extends MagnitudeBand("i", 780, "far red", MagnitudeSystem.AB)
  case object _z extends MagnitudeBand("z", 925, "near-infrared", MagnitudeSystem.AB)

  case object U  extends MagnitudeBand("U", 365, "ultraviolet")
  case object B  extends MagnitudeBand("B", 445, "blue")
  case object G  extends MagnitudeBand("G", 477)
  case object V  extends MagnitudeBand("V", 551, "visual")
  case object UC extends MagnitudeBand("UC",610, "UCAC") // unknown FWHM
  case object R  extends MagnitudeBand("R", 658, "red")
  case object I  extends MagnitudeBand("I", 806, "infrared")
  case object Z  extends MagnitudeBand("Z", 913)
  case object Y  extends MagnitudeBand("Y", 1020)
  case object J  extends MagnitudeBand("J", 1220)
  case object H  extends MagnitudeBand("H", 1630)
  case object K  extends MagnitudeBand("K", 2190)
  case object L  extends MagnitudeBand("L", 3450)
  case object M  extends MagnitudeBand("M", 4750)
  case object N  extends MagnitudeBand("N", 10000)
  case object Q  extends MagnitudeBand("Q", 16000)

  case object AP extends MagnitudeBand("AP", None, Some("apparent"), MagnitudeSystem.VEGA)

  val all: List[MagnitudeBand] =
    List(_u, _g, _r, _i, _z, U, B, G, V, UC, R, I, Z, Y, J, H, K, L, M, N, Q, AP)

  implicit val MagnitudeBandOrder: scalaz.Order[MagnitudeBand] =
    scalaz.Order.orderBy(_.wavelengthMidPointNm)

  implicit val MagnitudeBandOrdering: scala.math.Ordering[MagnitudeBand] =
    MagnitudeBandOrder.toScalaOrdering

  /** @group Typeclass Instances */
  implicit val equals = scalaz.Equal.equalA[MagnitudeBand]

}


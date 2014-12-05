package edu.gemini.spModel.core

sealed abstract class MagnitudeBand private (val name: String, val wavelengthMidPointNm: Int, val description: Option[String]) extends Product with Serializable {

  private def this(name: String, wavelengthMidPointNm: Int) =
    this(name, wavelengthMidPointNm, None)

  private def this(name: String, wavelengthMidPointNm: Int, description: String) =
    this(name, wavelengthMidPointNm, Some(description))

}

object MagnitudeBand {

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

  val all: List[MagnitudeBand] =
    List(U, B, G, V, UC, R, I, Z, Y, J, H, K, L, M, N, Q)

  implicit val MagnitudeBandOrder: scalaz.Order[MagnitudeBand] =
    scalaz.Order.orderBy(_.wavelengthMidPointNm)

  implicit val MagnitudeBandOrdering: scala.math.Ordering[MagnitudeBand] =
    MagnitudeBandOrder.toScalaOrdering

}


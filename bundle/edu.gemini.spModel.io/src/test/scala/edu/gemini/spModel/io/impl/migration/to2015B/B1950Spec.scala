package edu.gemini.spModel.io.impl.migration.to2015B

import edu.gemini.spModel.core.{Angle, Declination, RightAscension, Coordinates}

object B1950Spec extends App {

  def toJ2000(ra: Double, dec: Double): (Double, Double) = {
    val (ra0, dec0, _, _) = To2015B.toJ2000(ra, dec, 0, 0)
    (ra0, dec0)
  }

  sealed trait Catalog
  case object NED extends Catalog
  case object Simbad extends Catalog

  case class Result(cat: Catalog, ra: Double, dec: Double)

  // In B1950
  val cases = Map(
    "Vega"       -> Map((NED,    (278.812062, 38.738650,  279.232102, 38.782316)),
                        (Simbad, (278.81111,  38.73605,   279.234735, 38.783689))),
    "M51"        -> Map((NED,    (201.94300,  47.45294,   202.469575, 47.195258)),
                        (Simbad, (201.9578,   47.4882,    202.4842,   47.2306))),
    "Andromeda"  -> Map((NED,    (10.0004738, 40.9952444, 10.6847929, 41.2690650))),
    "Rigel"      -> Map((Simbad, (078.03330,  -8.25795,   78.634467, -08.201638))),
    "Betelgeuse" -> Map((Simbad, (088.11582,  07.39939,   88.792939,  07.407064)))
  )

  def hms(d: Double) = Angle.fromDegrees(d).formatHMS
  def dms(d: Double) = Angle.fromDegrees(d).formatDMS

  def as(a: Double, b: Double) = (a - b).abs * 60 * 60 * 60

  for {
    (name, cs)                  <- cases
    (cat, (ra, dec, ra0, dec0)) <- cs
  } {
    val (ra1, dec1) = toJ2000(ra, dec)
    println(f"$name%-10s $cat%-6s RA  $ra%10.6f -> $ra0%10.6f ${dms(ra0)}%13s")
    println(f"                                    $ra1%10.6f ${dms(ra1)}%13s ${as(ra1,ra0)}%7.3f as")
    println(f"                  DEC $dec%10.6f -> $dec0%10.6f ${hms(dec0)}%13s")
    println(f"                                    $dec1%10.6f ${hms(dec1)}%13s ${as(dec1, dec0)}%7.3f as")
    println()
  }

  println()
  val (x, y) = toJ2000(0, 0)
  println(s"Zero => ${dms(x)} ${dms(y)}")

}

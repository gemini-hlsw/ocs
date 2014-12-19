package edu.gemini.spModel.io.impl.migration.to2015B

import org.specs2.mutable.Specification

import edu.gemini.spModel.core.Angle

object B1950Spec extends Specification {

  def toJ2000(ra: Double, dec: Double): (Double, Double) = {
    val (ra0, dec0, _, _) = To2015B.toJ2000(ra, dec, 0, 0)
    (Angle.fromDegrees(ra0).toDegrees, Angle.fromDegrees(dec0).toDegrees)
  }

  def convert(ra0: String, dec0: String, ra1: String, dec1: String) =
    f"Convert Coordinates $ra0%9s $dec0%9s -> $ra1%12s $dec1%12s" in {

      val ra0d = Angle.parseHMS(ra0).toOption.get.toDegrees
      val ra1d = Angle.parseHMS(ra1).toOption.get.toDegrees

      val dec0d = Angle.parseDMS(dec0).toOption.get.toDegrees
      val dec1d = Angle.parseDMS(dec1).toOption.get.toDegrees

      val (ra2d, dec2d) = toJ2000(ra0d, dec0d)

      ra2d  must beCloseTo (ra1d,  0.00001)
      dec2d must beCloseTo (dec1d, 0.00001)

    }

  // Testcases from old OCS
  val cases = List(
    ( "0:0:0.0",   "0:0:0.0", "00:02:33.774",  "00:16:42.06"),
    ("02:0:0.0",  "40:0:0.0", "02:03:02.228",  "40:14:24.27"),
    ("08:0:0.0",  "20:0:0.0", "08:02:54.645",  "19:51:33.54"),
    ("10:0:0.0",  "60:0:0.0", "10:03:30.546",  "59:45:28.63"),
    ("16:0:0.0",  "80:0:0.0", "15:57:09.269",  "79:51:33.79"),
    ("22:0:0.0",  "40:0:0.0", "22:02:05.864",  "40:14:29.94"),
    ( "2:0:0.0", "-20:0:0.0", "02:02:21.575", "-19:45:34.66"),
    ( "8:0:0.0", "-40:0:0.0", "08:01:45.183", "-40:08:24.38"),
    ("10:0:0.0", "-60:0:0.0", "10:01:35.954", "-60:14:29.75"),
    ("16:0:0.0", "-80:0:0.0", "16:08:07.582", "-80:08:05.81"),
    ("22:0:0.0", "-40:0:0.0", "22:03:01.376", "-39:45:28.73")
  )

  "B1950 Conversion" should {
    cases foreach { case (a, b, c, d) => 
      convert(a, b, c, d)
    }
  }

}

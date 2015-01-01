package edu.gemini.spModel.io.impl.migration.to2015B

import org.specs2.mutable.Specification

import edu.gemini.spModel.core._

object B1950Spec extends Specification {

  case class Example(name: String, ra: String, dec: String, dra: Double, ddec: Double)
  case class Expected(ra: String, dec: String, dra: Double, ddec: Double, source: String)

  val cases: List[(Example, Expected, Expected)] = List(

    (Example("Polaris", "01:48:47.78", "+89:01:43.6", 276.655, -7.759),
      Expected("02:31:49.095", "+89:15:50.79", 44.48, -11.85, "simbad"),
      Expected("02:31:37.664", "+89:15:50.76", 45.17, -11.85, "jprecess")),

    (Example("HR 285", "01:01:30.68", "+85:59:24.3", 94.290, -7.275),
      Expected("01:08:44.880", "+86:15:25.52", 80.65, -11.54, "simbad"),
      Expected("01:08:41.026", "+86:15:25.54", 80.67, -11.54, "jprecess")),

    (Example("41 Dra", "18:03:53.50", "+79:59:59.5", 34.876, 127.638),
      Expected("18:00:09.206", "+80:00:14.76", 41.35, 127.70, "simbad"),
      Expected("18:00:08.546", "+80:00:14.77", 41.35, 127.70, "jprecess")),

    (Example("HR 1844", "05:33:01.57", "+75:00:53.6", 4.35, 26.019),
      Expected("05:39:43.704", "+75:02:37.94", -7.59, 25.67, "simbad"),
      Expected("05:39:43.774", "+75:02:37.92", -7.59, 25.67, "jprecess")),

    (Example("M51", "13:27:46.32", "+47:27:10.6", 0.0, 0.0),
      Expected("13:29:52.698", "+47:11:42.93", 0.00, 0.00, "simbad"),
      Expected("13:29:52.697", "+47:11:42.94", 0.00, 0.00, "jprecess")),

    (Example("Vega", "18:35:14.67", "+38:44:09.8", 197.23, 286.04),
      Expected("18:36:56.336", "+38:47:01.28", 200.94, 286.23, "simbad"),
      Expected("18:36:56.151", "+38:47:01.30", 200.94, 286.23, "jprecess")),

    (Example("Betelgeuse", "05:52:27.80",  "+07:23:57.8",  25.65, 11.44),
      Expected("05:55:10.305", "+07:24:25.43", 27.54, 11.30, "simbad"),
      Expected("05:55:10.309", "+07:24:25.42", 27.54, 11.30, "jprecess")),

    (Example("Barnard's Star", "17:55:22.70",  "+04:33:14.6",  -750.47, 10331.64),
      Expected("17:57:48.498", "+04:41:36.21", -798.58, 10328.12, "simbad"),
      Expected("17:57:48.502", "+04:41:36.24", -798.57, 10327.99, "jprecess")),

    (Example("Rigel", "05:12:07.99", "-08:15:28.6", -1.72, 1.27),
      Expected("05:14:32.272", "-08:12:05.90", 1.31, 0.50, "simbad"),
      Expected("05:14:32.269", "-08:12:05.86", 1.31, 0.50, "jprecess")),

    (Example("sig Oct", "20:15:03.45", "-89:08:18.4", 127.141, 5.008),
     Expected("21:08:46.839", "-88:57:23.40", 25.75, 4.98, "simbad"),
     Expected("21:08:42.998", "-88:57:21.16", 43.79, 4.88, "jprecess"))

  )

  val MasToDeg = 3600000

  def k45(q: Example, a: Expected, degTolerance: Double, pmTolerance: Double) =
    s"handle ${q.name}" in {

      val qra   = Angle.parseHMS(q.ra).toOption.get.toDegrees
      val qdec  = Angle.parseDMS(q.dec).toOption.get.toDegrees
      val qdra  = q.dra  / MasToDeg
      val qddec = q.ddec / MasToDeg

      val ra0   = Angle.parseHMS(a.ra).toOption.get.toDegrees
      val dec0  = Angle.parseDMS(a.dec).toOption.get.toDegrees
      val dra0  = a.dra / MasToDeg
      val ddec0 = a.ddec / MasToDeg

      val (ra1, dec1_, dra1, ddec1) = To2015B.toJ2000(qra, qdec, qdra, qddec)

      val dec1 = Angle.fromDegrees(dec1_).toDegrees

      val ra   = Angle.fromDegrees(ra1).formatHMS
      val dec  = Declination.fromAngle(Angle.fromDegrees(dec1)).get.formatDMS
      val dra  = dra1  * MasToDeg
      val ddec = ddec1 * MasToDeg

      ra1   must beCloseTo(ra0,   degTolerance)
      dec1  must beCloseTo(dec0,  degTolerance)
      dra1  must beCloseTo(dra0,  pmTolerance)
      ddec1 must beCloseTo(ddec0, pmTolerance)

    }


  "FK4 -> FK5 compared w/jprecess (almost exact)" should {
    cases foreach { case (q, _, a) => k45(q, a, 0.00001, 0.000001)}
  }

  "FK4 -> FK5 compared w/simbad (approximate)" should {
    cases foreach { case (q, a, _) => k45(q, a, 0.1, 0.00001)}
  }

}

package edu.gemini.spdb.rapidtoo.www

import java.io.BufferedReader
import java.security.Principal
import java.util
import java.util.Locale
import javax.servlet.{RequestDispatcher, ServletInputStream}
import javax.servlet.http.{Cookie, HttpSession, HttpServletRequest}

import edu.gemini.spModel.core.{Magnitude, Angle, Declination, RightAscension}
import edu.gemini.spdb.rapidtoo.TooGuideTarget
import edu.gemini.spdb.rapidtoo.TooGuideTarget.GuideProbe
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.shared.util.immutable.ScalaConverters._

import org.specs2.matcher.MatchResult
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._

object HttpTargetSpec extends Specification with ScalaCheck with Arbitraries with edu.gemini.spModel.core.Arbitraries {

  // a valid request
  val req = MockRequest(
    "target"   -> "banana",
    "ra"       -> "10",
    "dec"      -> "20",
    "gstarget" -> "manzana",
    "gsra"     -> "30",
    "gsdec"    -> "40",
    "gsprobe"  -> "PWFS1"
  )

  "HttpTooTarget" should {

    def mustFail(req: HttpServletRequest) =
      new HttpTooTarget(req) must throwA[BadRequestException]

    "Name: Parse" ! forAll { (s: String) =>
      s.nonEmpty ==> {
        val t = new HttpTooTarget(req.modifiedWith("target" -> s))
        t.getName must_== s
      }
    }
    "Name: Fail on Missing" in {
      mustFail(req without "target")
    }
    "Name: Fail on Empty" in {
      mustFail(req.modifiedWith("target" -> ""))
    }
    "RA: Parse Decimal" ! forAll { (ra: RightAscension) =>
      val t = new HttpTooTarget(req.modifiedWith("ra" -> ra.toAngle.toDegrees.toString))
      t.getRa must beCloseTo(ra.toAngle.toDegrees, .001)
    }
    "RA: Parse Sexigesimal" ! forAll { (ra: RightAscension) =>
      val t = new HttpTooTarget(req.modifiedWith("ra" -> ra.toAngle.formatHMS))
      t.getRa must beCloseTo(ra.toAngle.toDegrees, .001)
    }
    "RA: Fail on Malformed" in {
      mustFail(req.modifiedWith("ra" -> "cookie"))
    }
    "RA: Fail on Empty" in {
      mustFail(req.modifiedWith("ra" -> ""))
    }
    "RA: Fail on Missing" in {
      mustFail(req without "ra")
    }
    "Dec: Parse Decimal" ! forAll { (dec: Declination) =>
      val t = new HttpTooTarget(req.modifiedWith("dec" -> dec.toAngle.toDegrees.toString))
      t.getDec must beCloseTo(dec.toAngle.toDegrees, .001)
    }
    "Dec: Parse Sexigesimal" ! forAll { (dec: Declination) =>
      val t = new HttpTooTarget(req.modifiedWith("dec" -> dec.toAngle.formatDMS))
      Angle.fromDegrees(t.getDec).toDegrees must beCloseTo(dec.toAngle.toDegrees, .001)
    }
    "Dec: Fail on Malformed" in {
      mustFail(req.modifiedWith("dec" -> "roomba"))
    }
    "Dec: Fail on Empty" in {
      mustFail(req.modifiedWith("dec" -> ""))
    }
    "Dec: Fail on Missing" in {
      mustFail(req without "dec")
    }
    "Mags: Parse Magnitudes" ! forAll { (ms0: List[Magnitude]) =>
      val ms = ms0.map(_.copy(error = None))
      val s = ms.map(m => s"${m.value}/${m.band.name}/${m.system}").mkString(",")
      val t = new HttpTooTarget(req.modifiedWith("mags" -> s))
      t.getMagnitudes must_== ms.asImList
    }
    "Mags: Fail on Malformed" in {
      mustFail(req.modifiedWith("mags" -> "fluffy"))
    }

  }

  "HttpTooGuideTarget" should {

    def mustFail(req: HttpServletRequest) =
      HttpTooGuideTarget.parse(req) must throwA[BadRequestException]

    def mustEqual[A](t: GOption[TooGuideTarget], f: TooGuideTarget => A, expected: A): MatchResult[Any] =
      t.asScalaOpt.map(f) must_== Some(expected)

    def mustBeCloseTo(t: GOption[TooGuideTarget], f: TooGuideTarget => Double, expected: Angle): MatchResult[Any] =
      t.asScalaOpt.map(f).getOrElse(Double.MinValue) must beCloseTo(expected.toDegrees, 0.001)

    "Name: Parse" ! forAll { (s: String) =>
      s.nonEmpty ==> {
        val t = HttpTooGuideTarget.parse(req.modifiedWith("gstarget" -> s))
        mustEqual(t, _.getName, s)
      }
    }
    "Name: Default on Missing" in {
      val t = HttpTooGuideTarget.parse(req without "gstarget")
      mustEqual(t, _.getName, "GS")
    }
    "Name: Default on Empty" in {
      val t = HttpTooGuideTarget.parse(req.modifiedWith("gstarget" -> ""))
      mustEqual(t, _.getName, "GS")
    }
    "RA: Parse Decimal" ! forAll { (ra: RightAscension) =>
      val t = HttpTooGuideTarget.parse(req.modifiedWith("gsra" -> ra.toAngle.toDegrees.toString))
      mustBeCloseTo(t, _.getRa, ra.toAngle)
    }
    "RA: Parse Sexigesimal" ! forAll { (ra: RightAscension) =>
      val t = HttpTooGuideTarget.parse(req.modifiedWith("gsra" -> ra.toAngle.formatHMS))
      mustBeCloseTo(t, _.getRa, ra.toAngle)
    }
    "RA: Fail on Malformed" in {
      mustFail(req.modifiedWith("gsra" -> "cookie"))
    }
    "RA: Fail on Empty" in {
      mustFail(req.modifiedWith("gsra" -> ""))
    }
    "RA: Fail on Missing" in {
      mustFail(req without "gsra")
    }
    "Dec: Parse Decimal" ! forAll { (dec: Declination) =>
      val t = HttpTooGuideTarget.parse(req.modifiedWith("gsdec" -> dec.toAngle.toDegrees.toString))
      mustBeCloseTo(t, _.getDec, dec.toAngle)
    }
    "Dec: Parse Sexigesimal" ! forAll { (dec: Declination) =>
      val t = HttpTooGuideTarget.parse(req.modifiedWith("gsdec" -> dec.toAngle.formatDMS))
      mustBeCloseTo(t, t => Angle.fromDegrees(t.getDec).toDegrees, dec.toAngle)
    }
    "Dec: Fail on Malformed" in {
      mustFail(req.modifiedWith("gsdec" -> "roomba"))
    }
    "Dec: Fail on Empty" in {
      mustFail(req.modifiedWith("gsdec" -> ""))
    }
    "Dec: Fail on Missing" in {
      mustFail(req without "gsdec")
    }
    "Probe: Parse" ! forAll { (p: GuideProbe) =>
      val t = HttpTooGuideTarget.parse(req.modifiedWith("gsprobe" -> p.name))
      mustEqual(t, _.getGuideProbe, p)
    }
    "Probe: Fail on Malformed" in {
      mustFail(req.modifiedWith("gsprobe" -> "eskimo"))
    }
    "Probe: Fail on Empty" in {
      mustFail(req.modifiedWith("gsprobe" -> ""))
    }
    "Probe: Fail on Missing" in {
      mustFail(req without "gsprobe")
    }
    "Mags: Parse Magnitudes" ! forAll { (ms0: List[Magnitude]) =>
      val ms = ms0.map(_.copy(error = None))
      val s = ms.map(m => s"${m.value}/${m.band.name}/${m.system}").mkString(",")
      val t = HttpTooGuideTarget.parse(req.modifiedWith("gsmags" -> s))
      mustEqual(t, _.getMagnitudes, ms.asImList)
    }
    "Mags: Fail on Malformed" in {
      mustFail(req.modifiedWith("gsmags" -> "fluffy"))
    }
    "None: If no parameters" in {
      val req0 = req without "gstarget" without "gsra" without "gsdec" without "gsprobe"
      HttpTooGuideTarget.parse(req0) must_== None.asGeminiOpt
    }

  }

}
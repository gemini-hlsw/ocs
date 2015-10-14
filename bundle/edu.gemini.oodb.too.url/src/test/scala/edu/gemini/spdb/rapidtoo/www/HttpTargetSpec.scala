package edu.gemini.spdb.rapidtoo.www

import java.io.BufferedReader
import java.security.Principal
import java.util
import java.util.Locale
import javax.servlet.{RequestDispatcher, ServletInputStream}
import javax.servlet.http.{Cookie, HttpSession, HttpServletRequest}

import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.spModel.core.{Angle, Declination, RightAscension}
import edu.gemini.spdb.rapidtoo.TooGuideTarget.GuideProbe
import edu.gemini.shared.util.immutable.ScalaConverters._

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._

object HttpTargetSpec extends Specification with ScalaCheck with Arbitraries {

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
    "Mags: Parse Magnitudes" ! forAll { (ms: List[Magnitude]) =>
      val s = ms.map(m => s"${m.getBrightness}/${m.getBand}/${m.getSystem}").mkString(",")
      val t = new HttpTooTarget(req.modifiedWith("mags" -> s))
      t.getMagnitudes must_== ms.asImList
    }
    "Mags: Fail on Malformed" in {
      mustFail(req.modifiedWith("mags" -> "fluffy"))
    }

  }

  "HttpTooGuideTarget" should {

    def mustFail(req: HttpServletRequest) =
      new HttpTooGuideTarget(req) must throwA[BadRequestException]

    "Name: Parse" ! forAll { (s: String) =>
      s.nonEmpty ==> {
        val t = new HttpTooGuideTarget(req.modifiedWith("gstarget" -> s))
        t.getName must_== s
      }
    }
    "Name: Default on Missing" in {
      val t = new HttpTooGuideTarget(req without "gstarget")
      t.getName must_== "GS"
    }
    "Name: Default on Empty" in {
      val t = new HttpTooGuideTarget(req.modifiedWith("gstarget" -> ""))
      t.getName must_== "GS"
    }
    "RA: Parse Decimal" ! forAll { (ra: RightAscension) =>
      val t = new HttpTooGuideTarget(req.modifiedWith("gsra" -> ra.toAngle.toDegrees.toString))
      t.getRa must beCloseTo(ra.toAngle.toDegrees, .001)
    }
    "RA: Parse Sexigesimal" ! forAll { (ra: RightAscension) =>
      val t = new HttpTooGuideTarget(req.modifiedWith("gsra" -> ra.toAngle.formatHMS))
      t.getRa must beCloseTo(ra.toAngle.toDegrees, .001)
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
      val t = new HttpTooGuideTarget(req.modifiedWith("gsdec" -> dec.toAngle.toDegrees.toString))
      t.getDec must beCloseTo(dec.toAngle.toDegrees, .001)
    }
    "Dec: Parse Sexigesimal" ! forAll { (dec: Declination) =>
      val t = new HttpTooGuideTarget(req.modifiedWith("gsdec" -> dec.toAngle.formatDMS))
      Angle.fromDegrees(t.getDec).toDegrees must beCloseTo(dec.toAngle.toDegrees, .001)
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
      val t = new HttpTooGuideTarget(req.modifiedWith("gsprobe" -> p.name))
      t.getGuideProbe must_== p
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
    "Mags: Parse Magnitudes" ! forAll { (ms: List[Magnitude]) =>
      val s = ms.map(m => s"${m.getBrightness}/${m.getBand}/${m.getSystem}").mkString(",")
      val t = new HttpTooGuideTarget(req.modifiedWith("gsmags" -> s))
      t.getMagnitudes must_== ms.asImList
    }
    "Mags: Fail on Malformed" in {
      mustFail(req.modifiedWith("gsmags" -> "fluffy"))
    }

  }

  case class MockRequest(params: Map[String, String]) extends HttpServletRequest {

    def modifiedWith(more: (String, String)*): MockRequest =
      copy(params = params ++ more.toMap)

    def without(k: String): MockRequest =
      copy(params = params - k)

    def getParameter(k: String): String =
      params.get(k).orNull

    def getAttribute(p1: String): AnyRef = ???
    def getAttributeNames: util.Enumeration[_] = ???
    def getAuthType: String = ???
    def getCharacterEncoding: String = ???
    def getContentLength: Int = ???
    def getContentType: String = ???
    def getContextPath: String = ???
    def getCookies: Array[Cookie] = ???
    def getDateHeader(p1: String): Long = ???
    def getHeader(p1: String): String = ???
    def getHeaderNames: util.Enumeration[_] = ???
    def getHeaders(p1: String): util.Enumeration[_] = ???
    def getInputStream: ServletInputStream = ???
    def getIntHeader(p1: String): Int = ???
    def getLocale: Locale = ???
    def getLocales: util.Enumeration[_] = ???
    def getMethod: String = ???
    def getParameterMap: util.Map[_, _] = ???
    def getParameterNames: util.Enumeration[_] = ???
    def getParameterValues(p1: String): Array[String] = ???
    def getPathInfo: String = ???
    def getPathTranslated: String = ???
    def getProtocol: String = ???
    def getQueryString: String = ???
    def getReader: BufferedReader = ???
    def getRealPath(p1: String): String = ???
    def getRemoteAddr: String = ???
    def getRemoteHost: String = ???
    def getRemoteUser: String = ???
    def getRequestDispatcher(p1: String): RequestDispatcher = ???
    def getRequestedSessionId: String = ???
    def getRequestURI: String = ???
    def getRequestURL: StringBuffer = ???
    def getScheme: String = ???
    def getServerName: String = ???
    def getServerPort: Int = ???
    def getServletPath: String = ???
    def getSession(p1: Boolean): HttpSession = ???
    def getSession: HttpSession = ???
    def getUserPrincipal: Principal = ???
    def isRequestedSessionIdFromCookie: Boolean = ???
    def isRequestedSessionIdFromURL: Boolean = ???
    def isRequestedSessionIdFromUrl: Boolean = ???
    def isRequestedSessionIdValid: Boolean = ???
    def isSecure: Boolean = ???
    def isUserInRole(p1: String): Boolean = ???
    def removeAttribute(p1: String): Unit = ???
    def setAttribute(p1: String, p2: scala.Any): Unit = ???
    def setCharacterEncoding(p1: String): Unit = ???

  }

  object MockRequest {
    def apply(kvs: (String, String)*): MockRequest =
      apply(kvs.toMap)
  }

}
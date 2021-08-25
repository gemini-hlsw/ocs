package edu.gemini.itc.web.servlets

import argonaut._, Argonaut._
import edu.gemini.itc.shared._
import edu.gemini.itc.web.json.itcerror._
import edu.gemini.itc.web.json.itcparameters._
import edu.gemini.itc.web.json.itcresult._
import edu.gemini.itc.web.arb
import edu.gemini.json.disjunction._
import javax.servlet.http.HttpServletResponse
import org.scalacheck.{ Gen, Arbitrary }
import org.scalacheck.Arbitrary.arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalacheck.ScalaCheckParameters
import edu.gemini.spModel.gemini.gmos._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._

object JsonServletSpec extends Specification with ScalaCheck {

  // Arbitraries
  import arb.sourcedefinition._
  import arb.observationdetails._
  import arb.observingconditions._
  import arb.telescopedetails._
  import arb.instrumentdetails._
  import arb.itcparameters._

  // We only need one of these
  val servlet = new JsonServlet()

  "JsonServlet" should {

    "return SC_BAD_REQUEST with an error message in the case of malformed Json" in {

      val req = MockHttpServletRequest("blah")
      val res = MockHttpServletResponse()

      servlet.doPost(req, res)

      res.getContentType() mustEqual    "text/html; charset=UTF-8"
      res.getStatus()      mustEqual    HttpServletResponse.SC_BAD_REQUEST
      res.body.length      mustNotEqual 0

    }

    "return SC_BAD_REQUEST with an error message in the case of incorrect Json" in {

      val req = MockHttpServletRequest("[1, true, null]")
      val res = MockHttpServletResponse()

      servlet.doPost(req, res)
      println(res.body)

      res.getContentType() mustEqual    "text/html; charset=UTF-8"
      res.getStatus()      mustEqual    HttpServletResponse.SC_BAD_REQUEST
      res.body.length      mustNotEqual 0

    }

    "return SC_OK with a valid response for a valid request" !
      prop { (
        conditions:  ObservingConditions, // These can vary freely, the others stuff below needs
        telescope:   TelescopeDetails,    // to be tied down a bit otherwise we end up passing
        instrument:  InstrumentDetails    // stuff that's nonsensical and get an error back.
      ) =>

        val params = ItcParameters(
          source         = SourceDefinition(
            profile      = PointSource,
            distribution = LibraryStar.A0V,
            norm         = 500.0,
            units        = MagnitudeSystem.Vega,
            normBand     = MagnitudeBand.V,
            redshift     = Redshift.zero
          ),
          observation = ObservationDetails(
            calculationMethod = SpectroscopyS2N(
              exposures      = 10,
              coadds         = None,
              exposureTime   = 1.0,
              sourceFraction = 0.5,
              offset         = 0.0
            ),
            analysisMethod = AutoAperture(
              skyAperture  = 1.0
            )
          ),
          conditions  = conditions,
          telescope   = telescope,
          instrument  = GmosParameters(
            filter            = GmosNorthType.FilterNorth.g_G0301,
            grating           = GmosNorthType.DisperserNorth.R831_G5302,
            centralWavelength = Wavelength.fromNanometers(600),
            fpMask            = GmosNorthType.FPUnitNorth.LONGSLIT_4,
            ampGain           = GmosCommonType.AmpGain.HIGH,
            ampReadMode       = GmosCommonType.AmpReadMode.FAST,
            customSlitWidth   = None,
            spatialBinning    = 1,
            spectralBinning   = 1,
            ccdType           = GmosCommonType.DetectorManufacturer.HAMAMATSU,
            builtinROI        = GmosCommonType.BuiltinROI.FULL_FRAME,
            site              = Site.GN
          )
        )

        val req = MockHttpServletRequest(params.asJson.spaces2)
        val res = MockHttpServletResponse()

        // Do the things
        req.setCharacterEncoding("UTF-8")
        servlet.doPost(req, res)

        // Check Headers
        res.getStatus()      mustEqual    HttpServletResponse.SC_OK
        res.getContentType() mustEqual    "text/json; charset=UTF-8"

        // Ensure we can decode the result
        val result = Parse.decodeOption[ItcResult](res.body)
        result.isDefined mustEqual true

      }

  }

}

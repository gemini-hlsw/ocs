package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._
import edu.gemini.itc.web.arb
import edu.gemini.itc.service.ItcServiceImpl
import edu.gemini.spModel.gemini.gmos._
import edu.gemini.spModel.core._
import org.scalacheck.Arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object ItcSanitySpec extends Specification with ScalaCheck {

  // Arbitraries
  import arb.sourcedefinition._
  import arb.observationdetails._
  import arb.observingconditions._
  import arb.telescopedetails._
  import arb.instrumentdetails._
  import arb.itcparameters._

  // This is [logically] stateless so we should only need one.
  val itc = new ItcServiceImpl

  "ItcSanitySpec" should {

    // "provide a response for all input without any nasty crashing" !
    //   prop { (ps: ItcParameters) =>
    //     println("----")
    //     println(ps)
    //     println(itc.calculate(ps)) // just do this to ensure no throwing
    //     true
    //   }

    "provide a reasonable response for sensibe input" !
      prop { (
        source:      SourceDefinition,
        conditions:  ObservingConditions,
        telescope:   TelescopeDetails,
        instrument:  InstrumentDetails
      ) =>

        val params = ItcParameters(
          source      = source,
          observation = ObservationDetails(
            calculationMethod = SpectroscopyS2N(
              exposures      = 10,
              coadds         = None,
              exposureTime   = 1.0,
              sourceFraction = 0.5,
              offset         = 0.0
            ),
            analysisMethod = AutoAperture(
              skyAperture    = 1.0
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

        println("----")
        println(params)
        println(itc.calculate(params, true)) // just do this to ensure no throwing

        true

    }

  }

}

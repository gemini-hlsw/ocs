package edu.gemini.itc.web.igrins2

import edu.gemini.itc.base.SampledSpectrum
import edu.gemini.itc.igrins2.Igrins2Recipe
import edu.gemini.itc.service.ItcServiceImpl
import edu.gemini.itc.shared._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.telescope.IssPort
import org.specs2.mutable.Specification

import scalaz.{\/-, -\/}

/**
 * IGRINS-2 spectroscopy on a certain target and redshift produced a NaN in S/N charts.
 *
 * Target: QS02 quasar
 * Redshift: z ≈ 0.017594
 */
object Igrins2NaNReproSpec extends Specification {

  // Parameters for the failing observation
  // https://explore-dev.lucuma.xyz/p-11d8/observation/o-4cf7/target/t-149ab
  val rv = 5274600.0 // m/s
  val z = rv / 299792458.0 // ≈ 0.017594

  val conditions = new ObservingConditions(
    \/-(ImageQuality.PERCENT_70),
    \/-(CloudCover.PERCENT_50),
    WaterVapor.PERCENT_80,
    SkyBackground.PERCENT_80,
    2.0
  )

  val telescope = new TelescopeDetails(
    TelescopeDetails.Coating.SILVER,
    IssPort.SIDE_LOOKING,
    GuideProbe.Type.OIWFS
  )

  val source = SourceDefinition(
    PointSource,
    LibraryNonStar.QS02,
    11.315,
    MagnitudeSystem.Vega,
    MagnitudeBand.H,
    Redshift(z)
  )

  val igrins2Params = Igrins2Parameters(None)

  val observation = ObservationDetails(
    calculationMethod = SpectroscopyS2N(
      exposures      = 4,
      coadds         = None,
      exposureTime   = 141.0,
      sourceFraction = 1.0,
      offset         = 0.0,
      wavelengthAt   = Some(1.6)
    ),
    analysisMethod = AutoAperture(skyAperture = 1.0)
  )

  val params = ItcParameters(
    source      = source,
    observation = observation,
    conditions  = conditions,
    telescope   = telescope,
    instrument  = igrins2Params
  )

  private def nanCount(spectrum: SampledSpectrum): Int =
    (0 until spectrum.getLength).foldLeft(0) { (acc, i) =>
      val v = spectrum.getY(i)
      if (v.isNaN || v.isInfinite) acc + 1 else acc
    }

  "SC-8430 reproduction" should {

    "not produce NaN/Infinity in spectra" in {
      val recipe = new Igrins2Recipe(params, igrins2Params)
      val results = recipe.calculateSpectroscopy()

      results.foldLeft(0) { (acc, r) =>
        acc + nanCount(r.specS2N(0).getExpS2NSpectrum)
      } mustEqual 0

      results.foldLeft(0) { (acc, r) =>
        acc + nanCount(r.specS2N(0).getFinalS2NSpectrum)
      } mustEqual 0

      results.foldLeft(0) { (acc, r) =>
        acc + nanCount(r.specS2N(0).getSignalSpectrum)
      } mustEqual 0

      results.foldLeft(0) { (acc, r) =>
        acc + nanCount(r.specS2N(0).getBackgroundSpectrum)
      } mustEqual 0
    }

  }
}

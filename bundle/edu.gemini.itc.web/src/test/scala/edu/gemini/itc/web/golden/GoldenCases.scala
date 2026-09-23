package edu.gemini.itc.web.golden

import edu.gemini.itc.baseline._
import edu.gemini.itc.baseline.util.Fixture
import edu.gemini.itc.shared._
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.WavelengthConversions._
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos._
import edu.gemini.spModel.gemini.ghost.{GhostBinning, GhostReadNoiseGain}
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._
import edu.gemini.spModel.target.env.ResolutionMode
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.telescope.IssPort
import squants.motion.VelocityConversions._
import squants.radio.IrradianceConversions._
import squants.radio.SpectralIrradianceConversions._

import scalaz.\/-

/** A named ITC request whose full numeric output is pinned by [[ItcGoldenSpec]]. */
final case class GoldenCase(key: String, params: ItcParameters)

/**
 * The requests pinned by the golden spec and timed by the performance harness.
 *
 * Hand-built GMOS cases cover the expensive paths (Hamamatsu, IFU-2, integration time, user SED).
 * GHOST, F2 spectroscopy, GNIRS long slit and IGRINS-2 have hand-built cases too; the remaining
 * instruments are covered by a fixed pick from the baseline fixtures.
 */
object GoldenCases {

  private val goodConditions = ObservingConditions(
    iq      = \/-(ImageQuality.PERCENT_70),
    cc      = \/-(CloudCover.PERCENT_50),
    wv      = WaterVapor.ANY,
    sb      = SkyBackground.PERCENT_50,
    airmass = 1.5
  )

  private val telescope = new TelescopeDetails(
    TelescopeDetails.Coating.SILVER,
    IssPort.SIDE_LOOKING,
    GuideProbe.Type.OIWFS
  )

  private def a0v(mag: Double, band: MagnitudeBand, z: Double = 0.0) =
    SourceDefinition(PointSource, LibraryStar.A0V, mag, MagnitudeSystem.Vega, band, Redshift(z))

  // The calspec models span 100 nm to 32 um at very fine sampling, over one million points each.
  private def star(s: LibraryStar, mag: Double, band: MagnitudeBand, z: Double = 0.0) =
    SourceDefinition(PointSource, s, mag, MagnitudeSystem.Vega, band, Redshift(z))

  private def s2n(exposures: Int, exposureTime: Double, at: Option[Double]) =
    ObservationDetails(SpectroscopyS2N(exposures, None, exposureTime, 0.5, 0.0, at), AutoAperture(1.0))

  private def gmosN(grating: GmosNorthType.DisperserNorth, fpu: GmosNorthType.FPUnitNorth, cwl: Double,
                    ccd: GmosCommonType.DetectorManufacturer, binning: Int = 1) =
    GmosParameters(GmosNorthType.FilterNorth.NONE, grating, cwl.nm, fpu,
      GmosCommonType.AmpGain.LOW, GmosCommonType.AmpReadMode.SLOW, None, binning, binning, ccd,
      GmosCommonType.BuiltinROI.FULL_FRAME, Site.GN)

  private def gmosS(grating: GmosSouthType.DisperserSouth, fpu: GmosSouthType.FPUnitSouth, cwl: Double,
                    ccd: GmosCommonType.DetectorManufacturer, binning: Int = 1) =
    GmosParameters(GmosSouthType.FilterSouth.NONE, grating, cwl.nm, fpu,
      GmosCommonType.AmpGain.LOW, GmosCommonType.AmpReadMode.SLOW, None, binning, binning, ccd,
      GmosCommonType.BuiltinROI.FULL_FRAME, Site.GS)

  // A smooth synthetic spectrum covering the GMOS range and the R band, sampled every 0.5 nm.
  private lazy val syntheticUserSed: String = {
    val sb = new StringBuilder
    var wl = 300.0
    while (wl <= 1200.0) {
      val flux = 1.0e-15 * math.pow(wl / 600.0, -1.2) * (1.0 + 0.05 * math.sin(wl / 7.0))
      sb.append(wl).append(' ').append(flux).append('\n')
      wl += 0.5
    }
    sb.toString
  }

  lazy val gmos: List[GoldenCase] = List(
    GoldenCase("gmos-n-longslit-b1200-ham-s2n-a0v",
      ItcParameters(a0v(12.0, MagnitudeBand.R), s2n(10, 1.0, Some(600.0)), goodConditions, telescope,
        gmosN(GmosNorthType.DisperserNorth.B1200_G5301, GmosNorthType.FPUnitNorth.LONGSLIT_5, 600.0,
          GmosCommonType.DetectorManufacturer.HAMAMATSU))),

    GoldenCase("gmos-n-longslit-b1200-ham-s2n-a0v-calspec",
      ItcParameters(star(LibraryStar.A0V_new, 12.0, MagnitudeBand.R), s2n(10, 1.0, Some(600.0)), goodConditions, telescope,
        gmosN(GmosNorthType.DisperserNorth.B1200_G5301, GmosNorthType.FPUnitNorth.LONGSLIT_5, 600.0,
          GmosCommonType.DetectorManufacturer.HAMAMATSU))),

    GoldenCase("gmos-s-ifu2-r400-ham-inttime-g2v-calspec-z01",
      ItcParameters(star(LibraryStar.G2V_new, 18.0, MagnitudeBand.R, 0.1),
        ObservationDetails(SpectroscopyIntegrationTime(10.0, 700.0, None, 1.0, 0.0), IfuSum(250, 1.0, isIfu2 = true)),
        goodConditions, telescope,
        gmosS(GmosSouthType.DisperserSouth.R400_G5325, GmosSouthType.FPUnitSouth.IFU_1, 700.0,
          GmosCommonType.DetectorManufacturer.HAMAMATSU))),

    GoldenCase("gmos-n-longslit-r400-e2v-s2n-a0v-bin2",
      ItcParameters(a0v(18.0, MagnitudeBand.R), s2n(4, 300.0, Some(700.0)), goodConditions, telescope,
        gmosN(GmosNorthType.DisperserNorth.R400_G5305, GmosNorthType.FPUnitNorth.LONGSLIT_4, 700.0,
          GmosCommonType.DetectorManufacturer.E2V, binning = 2))),

    GoldenCase("gmos-s-longslit-r400-ham-inttime-a0v",
      ItcParameters(a0v(19.0, MagnitudeBand.R),
        ObservationDetails(SpectroscopyIntegrationTime(10.0, 700.0, None, 1.0, 0.0), AutoAperture(1.0)),
        goodConditions, telescope,
        gmosS(GmosSouthType.DisperserSouth.R400_G5325, GmosSouthType.FPUnitSouth.LONGSLIT_4, 700.0,
          GmosCommonType.DetectorManufacturer.HAMAMATSU))),

    GoldenCase("gmos-s-ifu2-r400-ham-s2n-ifusum-a0v",
      ItcParameters(a0v(17.0, MagnitudeBand.R),
        ObservationDetails(SpectroscopyS2N(3, None, 300.0, 1.0, 0.0, Some(700.0)), IfuSum(250, 1.0, isIfu2 = true)),
        goodConditions, telescope,
        gmosS(GmosSouthType.DisperserSouth.R400_G5325, GmosSouthType.FPUnitSouth.IFU_1, 700.0,
          GmosCommonType.DetectorManufacturer.HAMAMATSU))),

    GoldenCase("gmos-n-ifu2-b600-ham-s2n-ifuradial-a0v",
      ItcParameters(a0v(16.0, MagnitudeBand.R),
        ObservationDetails(SpectroscopyS2N(3, None, 300.0, 1.0, 0.0, Some(550.0)), IfuRadial(250, 0.0, 0.6)),
        goodConditions, telescope,
        gmosN(GmosNorthType.DisperserNorth.B600_G5307, GmosNorthType.FPUnitNorth.IFU_1, 550.0,
          GmosCommonType.DetectorManufacturer.HAMAMATSU))),

    GoldenCase("gmos-n-longslit-r831-ham-s2n-usersed",
      ItcParameters(
        SourceDefinition(PointSource, UserDefinedSpectrum("synthetic", syntheticUserSed), 17.0,
          MagnitudeSystem.Vega, MagnitudeBand.R, Redshift(0.0)),
        s2n(4, 300.0, Some(650.0)), goodConditions, telescope,
        gmosN(GmosNorthType.DisperserNorth.R831_G5302, GmosNorthType.FPUnitNorth.LONGSLIT_2, 650.0,
          GmosCommonType.DetectorManufacturer.HAMAMATSU))),

    GoldenCase("gmos-n-longslit-b600-ham-s2n-powerlaw-z03",
      ItcParameters(
        SourceDefinition(PointSource, PowerLaw(-1.5), 18.0, MagnitudeSystem.AB, MagnitudeBand.R, Redshift(0.3)),
        s2n(4, 300.0, Some(600.0)), goodConditions, telescope,
        gmosN(GmosNorthType.DisperserNorth.B600_G5307, GmosNorthType.FPUnitNorth.LONGSLIT_3, 600.0,
          GmosCommonType.DetectorManufacturer.HAMAMATSU))),

    GoldenCase("gmos-s-longslit-b600-ham-s2n-blackbody-gaussian",
      ItcParameters(
        SourceDefinition(GaussianSource(0.8), BlackBody(6000), 1.0e-3, MagnitudeSystem.Jy, MagnitudeBand.R, Redshift(0.1)),
        s2n(4, 300.0, Some(600.0)), goodConditions, telescope,
        gmosS(GmosSouthType.DisperserSouth.B600_G5323, GmosSouthType.FPUnitSouth.LONGSLIT_3, 600.0,
          GmosCommonType.DetectorManufacturer.HAMAMATSU))),

    GoldenCase("gmos-n-longslit-r400-ham-s2n-emissionline-usb",
      ItcParameters(
        SourceDefinition(UniformSource,
          EmissionLine(650.0.nm, 600.0.kps, 5.0e-18.wattsPerSquareMeter, 1.0e-17.wattsPerSquareMeterPerMicron),
          20.0, SurfaceBrightness.Vega, MagnitudeBand.R, Redshift(0.0)),
        s2n(4, 300.0, Some(650.0)), goodConditions, telescope,
        gmosN(GmosNorthType.DisperserNorth.R400_G5305, GmosNorthType.FPUnitNorth.LONGSLIT_4, 650.0,
          GmosCommonType.DetectorManufacturer.HAMAMATSU))),

    GoldenCase("gmos-n-imaging-e2v-inttime-a0v",
      ItcParameters(a0v(20.0, MagnitudeBand.R),
        ObservationDetails(ImagingIntegrationTime(25.0, None, 1.0, 0.0), AutoAperture(1.0)),
        goodConditions, telescope,
        GmosParameters(GmosNorthType.FilterNorth.r_G0303, GmosNorthType.DisperserNorth.MIRROR, 500.nm,
          GmosNorthType.FPUnitNorth.FPU_NONE, GmosCommonType.AmpGain.LOW, GmosCommonType.AmpReadMode.SLOW,
          None, 2, 2, GmosCommonType.DetectorManufacturer.E2V, GmosCommonType.BuiltinROI.FULL_FRAME, Site.GN))),

    GoldenCase("gmos-s-imaging-ham-s2n-a0v",
      ItcParameters(a0v(20.0, MagnitudeBand.R),
        ObservationDetails(ImagingS2N(5, None, 120.0, 1.0, 0.0), AutoAperture(1.0)),
        goodConditions, telescope,
        GmosParameters(GmosSouthType.FilterSouth.i_G0327, GmosSouthType.DisperserSouth.MIRROR, 500.nm,
          GmosSouthType.FPUnitSouth.FPU_NONE, GmosCommonType.AmpGain.LOW, GmosCommonType.AmpReadMode.SLOW,
          None, 1, 1, GmosCommonType.DetectorManufacturer.HAMAMATSU, GmosCommonType.BuiltinROI.FULL_FRAME, Site.GS)))
  )

  // GHOST is the one instrument whose dispersion is a table rather than a scalar.
  lazy val ghost: List[GoldenCase] = List(
    GoldenCase("ghost-standard-s2n-a0v",
      ItcParameters(a0v(15.0, MagnitudeBand.V), s2n(4, 600.0, Some(600.0)), goodConditions, telescope,
        GhostParameters(3, ResolutionMode.GhostStandard,
          GhostCameraParameters(GhostReadNoiseGain.SLOW_LOW, GhostBinning.ONE_BY_ONE, None),
          GhostCameraParameters(GhostReadNoiseGain.SLOW_LOW, GhostBinning.ONE_BY_ONE, None)))),

    GoldenCase("ghost-high-s2n-g2v-calspec",
      ItcParameters(star(LibraryStar.G2V_new, 12.0, MagnitudeBand.V), s2n(4, 300.0, Some(500.0)), goodConditions, telescope,
        GhostParameters(3, ResolutionMode.GhostHigh,
          GhostCameraParameters(GhostReadNoiseGain.MEDIUM_LOW, GhostBinning.ONE_BY_TWO, None),
          GhostCameraParameters(GhostReadNoiseGain.MEDIUM_LOW, GhostBinning.ONE_BY_TWO, None))))
  )

  private lazy val f2Longslit = {
    import Flamingos2._
    Flamingos2Parameters(Filter.J_LOW, Disperser.R1200JH, FPUnit.LONGSLIT_1, None, ReadMode.FAINT_OBJECT_SPEC)
  }

  private lazy val f2CustomMask = {
    import Flamingos2._
    Flamingos2Parameters(Filter.H, Disperser.R3000, FPUnit.CUSTOM_MASK, Some(CustomSlitWidth.CUSTOM_WIDTH_8_PIX),
      ReadMode.MEDIUM_OBJECT_SPEC)
  }

  private lazy val gnirsLongslit = {
    import GNIRSParams._
    GnirsParameters(PixelScale.PS_015, None, Some(Disperser.D_111), ReadMode.VERY_BRIGHT, CrossDispersed.NO,
      2.4.microns, SlitWidth.SW_3, None, WellDepth.SHALLOW, None)
  }

  lazy val nir: List[GoldenCase] = List(
    GoldenCase("f2-longslit-r1200jh-s2n-a0v",
      ItcParameters(a0v(12.0, MagnitudeBand.H), s2n(6, 300.0, None), goodConditions, telescope, f2Longslit)),

    GoldenCase("f2-custommask-r3000-s2n-a0v",
      ItcParameters(a0v(12.0, MagnitudeBand.H), s2n(6, 300.0, None), goodConditions, telescope, f2CustomMask)),

    GoldenCase("gnirs-longslit-d111-s2n-a0v",
      ItcParameters(a0v(12.0, MagnitudeBand.K), s2n(6, 300.0, None), goodConditions, telescope, gnirsLongslit)),

    // wavelengthAt is in nm here; the service converts IGRINS-2 requests to microns
    GoldenCase("igrins2-s2n-a0v",
      ItcParameters(a0v(10.0, MagnitudeBand.H), s2n(4, 141.0, Some(1650.0)), goodConditions, telescope,
        Igrins2Parameters(None)))
  )

  // Two fixed picks per instrument from the baseline fixtures: the first one and one from the middle.
  private def fromBaseline[T <: InstrumentDetails](name: String, fs: List[Fixture[T]]): List[GoldenCase] =
    List(0, fs.size / 2).distinct.map { i =>
      val f = fs(i)
      GoldenCase(s"baseline-$name-$i-${f.hash}", ItcParameters(f.src, f.odp, f.ocp, f.tep, f.ins))
    }

  lazy val baseline: List[GoldenCase] =
    fromBaseline("acqcam",   BaselineAcqCam.Fixtures) ++
    fromBaseline("f2",       BaselineF2.Fixtures) ++
    fromBaseline("gmos",     BaselineGmos.Fixtures) ++
    fromBaseline("gnirs",    BaselineGnirs.Fixtures) ++
    fromBaseline("gsaoi",    BaselineGsaoi.Fixtures) ++
    fromBaseline("nifs",     BaselineNifs.Fixtures) ++
    fromBaseline("niri",     BaselineNiri.Fixtures)

  lazy val all: List[GoldenCase] = gmos ++ ghost ++ nir ++ baseline

  def byKey(key: String): Option[GoldenCase] = all.find(_.key == key)

}

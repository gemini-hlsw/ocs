package edu.gemini.itc.baseline.util

import edu.gemini.itc.gnirs.GnirsParameters
import edu.gemini.itc.michelle.MichelleParameters
import edu.gemini.itc.nifs.NifsParameters
import edu.gemini.itc.shared._
import edu.gemini.itc.trecs.TRecsParameters
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.CrossDispersed
import edu.gemini.spModel.target._

// TEMPORARY helper
// All input objects will become immutable data only objects (probably Scala case classes).
// For now we need a workaround for missing hash functions on the existing Java objects.
object Hash {

  def calc(ip: InstrumentDetails): Int = ip match {
    case p: AcquisitionCamParameters  => calc(p)
    case p: Flamingos2Parameters      => calc(p)
    case p: GmosParameters            => calc(p)
    case p: GnirsParameters           => calc(p)
    case p: GsaoiParameters           => calc(p)
    case p: MichelleParameters        => calc(p)
    case p: NifsParameters            => calc(p)
    case p: NiriParameters            => calc(p)
    case p: TRecsParameters           => calc(p)
    case _                            => throw new Exception("no hash function available")
  }

  def calc(p: GmosParameters): Int =
    hash(
      p.filter.name,
      p.ccdType.name,
      p.fpMask.name,
      p.grating.name,
      p.ifuMethod.toString,
      p.centralWavelength,
      p.site.name,
      p.spatialBinning,
      p.spectralBinning
    )

  def calc(p: GnirsParameters): Int =
    hash(
      if (correctedCentralWavelength(p) < 2600) "BC" else "RC",
      if (p.getPixelScale.equals(GNIRSParams.PixelScale.PS_005)) "L" else "S" ,
      p.getSlitWidth match {
        case GNIRSParams.SlitWidth.SW_1   => "slit0.10"
        case GNIRSParams.SlitWidth.SW_3   => "slit0.20"
        case GNIRSParams.SlitWidth.SW_6   => "slit0.675"
        case GNIRSParams.SlitWidth.SW_8   => "slit3.0"
      },
      p.getGrating match {
        case GNIRSParams.Disperser.D_10   => "G10"
        case GNIRSParams.Disperser.D_32   => "G32"
        case GNIRSParams.Disperser.D_111  => "G110"
      },
      correctedCentralWavelength(p),
      p.getReadMode match {
        case GNIRSParams.ReadMode.BRIGHT      => "medNoise"
        case GNIRSParams.ReadMode.FAINT       => "lowNoise"
        case GNIRSParams.ReadMode.VERY_FAINT  => "verylowNoise"
        case GNIRSParams.ReadMode.VERY_BRIGHT => "highNoise"
      },
    // TODO: remove entirely!
      p.getSlitWidth match {
        case GNIRSParams.SlitWidth.SW_1 => "010"
        case GNIRSParams.SlitWidth.SW_2 => "015"
        case GNIRSParams.SlitWidth.SW_3 => "020"
        case GNIRSParams.SlitWidth.SW_4 => "030"
        case GNIRSParams.SlitWidth.SW_5 => "045"
        case GNIRSParams.SlitWidth.SW_6 => "0675"
        case GNIRSParams.SlitWidth.SW_7 => "100"
        case GNIRSParams.SlitWidth.SW_8 => "300"
      },
      p.getCentralWavelength.toNanometers
    )

  // TODO: remove this
  private def correctedCentralWavelength(p: GnirsParameters): Double = {
    if (p.getCrossDispersed == CrossDispersed.NO) {
      p.getCentralWavelength.toNanometers
    }
    else {
      1616.85
    }
  }
  // ==

  def calc(p: GsaoiParameters): Int =
    hash(
      p.filter.name,
      p.readMode.name
    )

  def calc(p: MichelleParameters): Int =
    hash(
      p.getFilter,
      p.getFocalPlaneMask.name,
      p.getGrating,
      p.getInstrumentCentralWavelength,
      p.polarimetryIsUsed()
    )

  def calc(p: NifsParameters): Int =
    hash(
      p.getFilter,
      p.getFPMask,
      p.getGrating,
      p.getIFUMaxOffset,
      p.getIFUMethod,
      p.getIFUMinOffset,
      p.getIFUOffset,
      p.getInstrumentCentralWavelength,
      p.getReadNoise,
      p.getUnXDispCentralWavelength
    )

  def calc(p: NiriParameters): Int =
    hash(
      p.camera.name,
      p.filter.name,
      p.mask.name,
      p.grism.name,
      p.readMode.name,
      p.wellDepth.name
    )

  def calc(p: TRecsParameters): Int =
    hash(
      p.getFilter,
      p.getFocalPlaneMask,
      p.getGrating,
      p.getInstrumentCentralWavelength,
      p.getInstrumentWindow
    )

  def calc(p: AcquisitionCamParameters): Int =
    hash(
      p.colorFilter.name,
      p.ndFilter.name
    )

  def calc(p: Flamingos2Parameters): Int =
    hash(
      p.filter.name,
      p.mask.name,
      p.grism.name,
      p.readMode.name
    )

  def calc(odp: ObservationDetails): Int =
    hash(
      odp.isAutoAperture,
      odp.getMethod.isS2N,
      odp.getMethod.isImaging,
      odp.getExposureTime,
      odp.getNumExposures,
      odp.getApertureDiameter,
      odp.getSkyApertureDiameter,
      odp.getSNRatio,
      odp.getSourceFraction
    )

  // TODO: simplify this again once refactoring of source/source profile/source distribution is done
  def calc(src: SourceDefinition): Int =
    hash(
      src.getProfileType.name,
      src.getDistributionType.name,
      src.profile match {
        case s: GaussianSource => s.fwhm
        case _                 => 0.0
      },
      src.distribution match {
        case d: BlackBody       => d.temperature
        case d: PowerLaw        => d.index
        case d: EmissionLine    => (d.wavelength.toMicrons * 1000 + d.continuum.toWatts) * 1000 + d.flux.toWatts
        case d: Library         => d.sedSpectrum
      },
      src.norm,               // this is the magnitude value
      src.normBand.name,      // this is the magnitude band name
      src.redshift
    )

  def calc(tp: TelescopeDetails): Int =
    hash(
      tp.getInstrumentPort.displayValue,
      tp.getMirrorCoating.displayValue,
      tp.getWFS.name.toLowerCase
    )

  def calc(ocp: ObservingConditions): Int =
    hash(
      ocp.getAirmass,
      ocp.getImageQuality,
      ocp.getSkyTransparencyCloud,
      ocp.getSkyTransparencyWater,
      ocp.getSkyBackground
    )

  def calc(alt: AltairParameters): Int =
    hash(
      alt.guideStarMagnitude,
      alt.guideStarSeparation,
      alt.fieldLens.displayValue,
      alt.wfsMode.displayValue
    )

  def calc(alt: GemsParameters): Int =
    hash(
      alt.avgStrehl,
      alt.strehlBand
    )

  def calc(pdp: PlottingDetails): Int =
    hash(pdp.getPlotWaveL, pdp.getPlotWaveU)

  private def hash(values: Any*) =
    values.
      filter(_ != null).
      map(_.hashCode).
      foldLeft(17)((acc, h) => 37*acc + h)

}


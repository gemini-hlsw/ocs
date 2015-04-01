package edu.gemini.itc.baseline.util

import edu.gemini.itc.acqcam.AcquisitionCamParameters
import edu.gemini.itc.altair.AltairParameters
import edu.gemini.itc.flamingos2.Flamingos2Parameters
import edu.gemini.itc.gems.GemsParameters
import edu.gemini.itc.gnirs.GnirsParameters
import edu.gemini.itc.gsaoi.GsaoiParameters
import edu.gemini.itc.michelle.MichelleParameters
import edu.gemini.itc.nifs.NifsParameters
import edu.gemini.itc.niri.NiriParameters
import edu.gemini.itc.shared._
import edu.gemini.itc.trecs.TRecsParameters

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
      p.getCameraColor,
      p.getCameraLength,
      p.getDarkCurrent,
      p.getFocalPlaneMask,
      gnirsFpMask(p),
      p.getGrating,
      p.getInstrumentCentralWavelength,
      p.getReadNoise,
      p.getStringSlitWidth,
      p.getUnXDispCentralWavelength
    )

  // TODO: get rid of with next update
  private def gnirsFpMask(p: GnirsParameters) = p.getFocalPlaneMask match {
    case GnirsParameters.SLIT0_1        => 0.1
    case GnirsParameters.SLIT0_15       => 0.15
    case GnirsParameters.SLIT0_2        => 0.2
    case GnirsParameters.SLIT0_3        => 0.3
    case GnirsParameters.SLIT0_45       => 0.45
    case GnirsParameters.SLIT0_675      => 0.675
    case GnirsParameters.SLIT1_0        => 1.0
    case GnirsParameters.SLIT3_0        => 3.0
    case _                              => -1.0
  }

  def calc(p: GsaoiParameters): Int =
    hash(
      p.getFilter,
      p.getReadMode
    )

  def calc(p: MichelleParameters): Int =
    hash(
      p.getFilter,
      p.getFocalPlaneMask,
      getFPMask(p),               // TODO: get rid of with next update
      p.getGrating,
      p.getInstrumentCentralWavelength,
      getStringSlitWidth(p),      // TODO: get rid of with next update
      p.polarimetryIsUsed()
    )

  // TODO: get rid of with next update
  def getFPMask(p: MichelleParameters): Double = p.getFocalPlaneMask match {
    case MichelleParameters.SLIT0_19 => 0.19
    case MichelleParameters.SLIT0_38 => 0.38
    case MichelleParameters.SLIT0_57 => 0.57
    case MichelleParameters.SLIT0_76 => 0.76
    case MichelleParameters.SLIT1_52 => 1.52
    case _                           => -1.0
  }

  // TODO: get rid of with next update
  def getStringSlitWidth(p: MichelleParameters): String = p.getFocalPlaneMask match {
    case MichelleParameters.SLIT0_19 => "019"
    case MichelleParameters.SLIT0_38 => "038"
    case MichelleParameters.SLIT0_57 => "057"
    case MichelleParameters.SLIT0_76 => "076"
    case MichelleParameters.SLIT1_52 => "152"
    case _                           => "none"
  }

  def calc(p: NifsParameters): Int =
    hash(
      p.getFilter,
      p.getFPMask,
      p.getGrating,
//      p.getIFUCenterX,          // TODO: results in NPE if not set..
//      p.getIFUCenterY,
      p.getIFUMaxOffset,
      p.getIFUMethod,
      p.getIFUMinOffset,
//      p.getIFUNumX,             // TODO: results in NPE if not set..
//      p.getIFUNumY,
      p.getIFUOffset,
      p.getInstrumentCentralWavelength,
      p.getReadNoise,
      p.getUnXDispCentralWavelength
    )

  def calc(p: NiriParameters): Int =
    hash(
      p.getCamera,
      p.getFilter,
      p.getFocalPlaneMask,
//      p.getFPMaskOffset,
      p.getGrism,
      p.getReadNoise,
//      p.getStringSlitWidth,
      p.getWellDepth
    )

  def calc(p: TRecsParameters): Int =
    hash(
      p.getFilter,
      p.getFocalPlaneMask,
      getFPMask(p),                 // TODO: remove with next update of baseline
      p.getGrating,
      p.getInstrumentCentralWavelength,
      p.getInstrumentWindow,
      getStringSlitWidth(p)         // TODO: remove with next update of baseline
    )

  // TODO: remove with next update of baseline
  def getFPMask(p: TRecsParameters): Double = p.getFocalPlaneMask match {
    case TRecsParameters.SLIT0_21 => 0.21
    case TRecsParameters.SLIT0_26 => 0.26
    case TRecsParameters.SLIT0_31 => 0.31
    case TRecsParameters.SLIT0_36 => 0.36
    case TRecsParameters.SLIT0_66 => 0.66
    case TRecsParameters.SLIT0_72 => 0.72
    case TRecsParameters.SLIT1_32 => 1.32
    case _ => -1.0
  }

  // TODO: remove with next update of baseline
  def getStringSlitWidth(p: TRecsParameters): String = p.getFocalPlaneMask match {
    case TRecsParameters.SLIT0_21 => "021"
    case TRecsParameters.SLIT0_26 => "026"
    case TRecsParameters.SLIT0_31 => "031"
    case TRecsParameters.SLIT0_26 => "036"
    case TRecsParameters.SLIT0_66 => "066"
    case TRecsParameters.SLIT0_72 => "072"
    case TRecsParameters.SLIT1_32 => "132"
    case _ => "none"
  }

  def calc(p: AcquisitionCamParameters): Int =
    hash(
      p.getColorFilter,
      p.getNDFilter
    )

  def calc(p: Flamingos2Parameters): Int =
    hash(
      p.getColorFilter,
      p.getFPMask,
      p.getGrism,
      p.getReadNoise,
      if (p.getFPMask.equalsIgnoreCase("none")) 1.0 else p.getFPMask.toDouble // TODO: remove with next update
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

  def calc(src: SourceDefinition): Int =
    hash(
      src.getProfileType.name,
      src.profile.norm,
      src.profile.units.name,
      src.distribution,
      src.normBand.name,
      src.redshift
    )

  def calc(tp: TelescopeDetails): Int =
    hash(
      tp.getInstrumentPort.displayValue,
      tp.getMirrorCoating.displayValue,
      tp.getWFS.displayValue
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
      alt.getGuideStarMagnitude,
      alt.getGuideStarSeperation,
      alt.getFieldLens.displayValue,
      alt.getWFSMode.displayValue
    )

  def calc(alt: GemsParameters): Int =
    hash(
      alt.getAvgStrehl,
      alt.getStrehlBand
    )

  def calc(pdp: PlottingDetails): Int =
    hash(pdp.getPlotWaveL, pdp.getPlotWaveU)

  private def hash(values: Any*) =
    values.
      filter(_ != null).
      map(_.hashCode).
      foldLeft(17)((acc, h) => 37*acc + h)

}


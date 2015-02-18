package edu.gemini.itc.baseline.util

import edu.gemini.itc.acqcam.AcquisitionCamParameters
import edu.gemini.itc.altair.AltairParameters
import edu.gemini.itc.flamingos2.Flamingos2Parameters
import edu.gemini.itc.gems.GemsParameters
import edu.gemini.itc.gmos.GmosParameters
import edu.gemini.itc.gnirs.GnirsParameters
import edu.gemini.itc.gsaoi.GsaoiParameters
import edu.gemini.itc.michelle.MichelleParameters
import edu.gemini.itc.nifs.NifsParameters
import edu.gemini.itc.niri.NiriParameters
import edu.gemini.itc.parameters.SourceDefinitionParameters.Distribution
import edu.gemini.itc.parameters.SourceDefinitionParameters.Distribution._
import edu.gemini.itc.parameters.{ObservationDetailsParameters, ObservingConditionParameters, SourceDefinitionParameters, TeleParameters}
import edu.gemini.itc.shared._
import edu.gemini.itc.trecs.TRecsParameters

// TEMPORARY helper
// All input objects will become immutable data only objects (probably Scala case classes).
// For now we need a workaround for missing hash functions on the existing Java objects.
object Hash {

  def calc(ip: ITCParameters): Int = ip match {
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
      p.getFilter,
      p.getCCDtype,
      p.getDarkCurrent,
      p.getFocalPlaneMask,
      p.getFPMask,
      p.getGrating,
      p.getIFUMaxOffset,
      p.getIFUMethod,
      p.getIFUMinOffset,
      p.getIFUOffset,
      p.getInstrumentCentralWavelength,
      p.getInstrumentLocation,
      p.getReadNoise,
      p.getSpatialBinning,
      p.getSpectralBinning,
      p.getStringSlitWidth,
      p.getWellDepth
    )

  def calc(p: GnirsParameters): Int =
    hash(
      p.getCameraColor,
      p.getCameraLength,
      p.getDarkCurrent,
      p.getFocalPlaneMask,
      p.getFPMask,
      p.getGrating,
      p.getInstrumentCentralWavelength,
      p.getReadNoise,
      p.getStringSlitWidth,
      p.getUnXDispCentralWavelength
    )

  def calc(p: GsaoiParameters): Int =
    hash(
      p.getCamera,
      p.getFilter,
      p.getReadMode
    )

  def calc(p: MichelleParameters): Int =
    hash(
      p.getFilter,
      p.getFocalPlaneMask,
      p.getFPMask,
      p.getGrating,
      p.getInstrumentCentralWavelength,
      p.getSpatialBinning,
      p.getSpectralBinning,
      p.getStringSlitWidth,
      p.polarimetryIsUsed()
    )

  def calc(p: NifsParameters): Int =
    hash(
      p.getDarkCurrent,
      p.getFilter,
      p.getFocalPlaneMask,
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
      p.getStringSlitWidth,
      p.getUnXDispCentralWavelength
    )

  def calc(p: NiriParameters): Int =
    hash(
      p.getCamera,
      p.getFilter,
      p.getFocalPlaneMask,
//      p.getFPMask,
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
      p.getFPMask,
      p.getGrating,
      p.getInstrumentCentralWavelength,
      p.getInstrumentWindow,
      p.getSpatialBinning,
      p.getSpectralBinning,
      p.getStringSlitWidth
    )

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
      p.getSlitSize
    )

  def calc(odp: ObservationDetailsParameters): Int =
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

  def calc(src: SourceDefinitionParameters): Int =
    hash(
      src.getProfileType.name,
      src.profile.norm,
      src.profile.units.name,
      src.distribution,
      src.normBand.name,
      src.redshift
    )

  def calc(tp: TeleParameters): Int =
    hash(
      tp.getInstrumentPort.displayValue,
      tp.getMirrorCoating.displayValue,
      tp.getWFS.displayValue
    )

  def calc(ocp: ObservingConditionParameters): Int =
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
      alt.gemsIsUsed,
      alt.getAvgStrehl,
      alt.getStrehlBand
    )

  private def hash(values: Any*) =
    values.
      filter(_ != null).
      map(_.hashCode).
      foldLeft(17)((acc, h) => 37*acc + h)

}


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
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.niri.Niri

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
      p.getFocalPlaneMask,
      p.getGrating,
      p.getInstrumentCentralWavelength,
      p.getReadNoise,
      p.getStringSlitWidth,
      p.getUnXDispCentralWavelength
    )

  def calc(p: GsaoiParameters): Int =
    hash(
      p.getFilter,
      p.getReadMode
    )

  def calc(p: MichelleParameters): Int =
    hash(
      p.getFilter,
      p.getFocalPlaneMask,
      p.getGrating,
      p.getInstrumentCentralWavelength,
      p.polarimetryIsUsed()
    )

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
      p.getFilter match {                     // TODO: cleanup with next baseline update
        case Niri.Filter.BBF_J  => "J"
        case Niri.Filter.BBF_K  => "K"
        case Niri.Filter.BBF_H  => "H"
        case f                  => f.name()
      },
      p.getFocalPlaneMask,
      p.getGrism,
      p.getReadNoise,
      p.getWellDepth
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
      p.getColorFilter,
      p.getNDFilter
    )

  def calc(p: Flamingos2Parameters): Int =
    hash(
      f2FilterToName(p.getFilter),                                                                        // TODO: cleanup with next baseline update
      if (p.getFPMask.equals(Flamingos2.FPUnit.FPU_NONE)) "none" else p.getFPMask.getSlitWidth.toString,  // TODO: cleanup with next baseline update
      f2DisperserToName(p.getGrism),                                                                      // TODO: cleanup with next baseline update
      p.getReadMode match {                                                                               // TODO: cleanup with next baseline update
        case Flamingos2.ReadMode.BRIGHT_OBJECT_SPEC   => "highNoise"
        case Flamingos2.ReadMode.MEDIUM_OBJECT_SPEC   => "medNoise"
        case Flamingos2.ReadMode.FAINT_OBJECT_SPEC    => "lowNoise"
      }
    )

  private def f2FilterToName(filter: Flamingos2.Filter): String = filter match {
    case Flamingos2.Filter.OPEN         => "Open"
    case Flamingos2.Filter.H            => "H_G0803"
    case Flamingos2.Filter.J_LOW        => "Jlow_G0801"
    case _                              => filter.name()
  }
  private def f2DisperserToName(filter: Flamingos2.Disperser): String = filter match {
    case Flamingos2.Disperser.NONE      => "None"
    case Flamingos2.Disperser.R1200HK   => "HK_G5802"
    case Flamingos2.Disperser.R1200JH   => "JH_G5801"
    case Flamingos2.Disperser.R3000     => "R3K_G5803"
  }

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


package edu.gemini.itc.shared

import edu.gemini.spModel.core.{Wavelength, Site}
import edu.gemini.spModel.gemini.acqcam.AcqCamParams
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosCommonType
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.niri.Niri

/*
 * A collection of objects that define subsets of instrument configuration parameters
 * which are needed for ITC calculations of the corresponding instruments.
 */

sealed trait InstrumentDetails

final case class AcquisitionCamParameters(
                     colorFilter:       AcqCamParams.ColorFilter,
                     ndFilter:          AcqCamParams.NDFilter) extends InstrumentDetails

final case class Flamingos2Parameters(
                     filter:            Flamingos2.Filter,
                     grism:             Flamingos2.Disperser,
                     mask:              Flamingos2.FPUnit,
                     readMode:          Flamingos2.ReadMode) extends InstrumentDetails

final case class GmosParameters(
                     filter:            GmosCommonType.Filter,
                     grating:           GmosCommonType.Disperser,
                     centralWavelength: Wavelength,
                     fpMask:            GmosCommonType.FPUnit,
                     customSlitWidth:   Option[GmosCommonType.CustomSlitWidth],
                     spatialBinning:    Int,
                     spectralBinning:   Int,
                     ifuMethod:         Option[IfuMethod],
                     ccdType:           GmosCommonType.DetectorManufacturer,
                     site:              Site) extends InstrumentDetails

final case class GnirsParameters(
                     pixelScale:        GNIRSParams.PixelScale,
                     grating:           GNIRSParams.Disperser,
                     readMode:          GNIRSParams.ReadMode,
                     crossDispersed:    GNIRSParams.CrossDispersed,
                     centralWavelength: Wavelength,
                     slitWidth:         GNIRSParams.SlitWidth) extends InstrumentDetails

final case class GsaoiParameters(
                      filter:           Gsaoi.Filter,
                      readMode:         Gsaoi.ReadMode,
                      gems:             GemsParameters) extends InstrumentDetails

final case class NiriParameters(
                     filter:            Niri.Filter,
                     grism:             Niri.Disperser,
                     camera:            Niri.Camera,
                     readMode:          Niri.ReadMode,
                     wellDepth:         Niri.WellDepth,
                     mask:              Niri.Mask,
                     altair:            Option[AltairParameters]) extends InstrumentDetails


// == AO

final case class AltairParameters(
                     guideStarSeparation: Double,
                     guideStarMagnitude:  Double,
                     fieldLens:           AltairParams.FieldLens,
                     wfsMode:             AltairParams.GuideStarType)

final case class GemsParameters(
                     avgStrehl:           Double,
                     strehlBand:          String)


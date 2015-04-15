package edu.gemini.itc.shared

import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.acqcam.AcqCamParams
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosCommonType

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
                     centralWavelength: Double,
                     fpMask:            GmosCommonType.FPUnit,
                     spatialBinning:    Int,
                     spectralBinning:   Int,
                     ifuMethod:         Option[IfuMethod],
                     ccdType:           GmosCommonType.DetectorManufacturer,
                     site:              Site) extends InstrumentDetails


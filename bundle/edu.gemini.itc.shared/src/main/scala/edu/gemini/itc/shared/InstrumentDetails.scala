package edu.gemini.itc.shared

import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.gmos.GmosCommonType.{DetectorManufacturer, FPUnit, Disperser, Filter}

trait InstrumentDetails

final case class GmosParameters(
                     filter: Filter,
                     grating: Disperser,
                     centralWavelength: Double,
                     fpMask: FPUnit,
                     spatialBinning: Int,
                     spectralBinning: Int,
                     ifuMethod: Option[IfuMethod],
                     ccdType: DetectorManufacturer,
                     site: Site) extends InstrumentDetails

object InstrumentDetails {

}

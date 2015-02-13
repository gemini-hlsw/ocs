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
import edu.gemini.itc.parameters.ObservationDetailsParameters
import edu.gemini.itc.shared.ITCParameters
import edu.gemini.itc.trecs.TRecsParameters

/**
 * Observations combine some observation details and instrument configurations.
 * These two aspects have to be handled as one because certain observation types ask for specific instrument
 * configurations and vice versa.
 */
sealed trait Observation {
  val odp: ObservationDetailsParameters
  val ins: ITCParameters
  val hash = Hash.calc(odp)*37 + Hash.calc(ins)
}
case class AcqCamObservation(odp: ObservationDetailsParameters, ins: AcquisitionCamParameters) extends Observation
case class F2Observation(odp: ObservationDetailsParameters, ins: Flamingos2Parameters) extends Observation
case class GmosObservation(odp: ObservationDetailsParameters, ins: GmosParameters) extends Observation
case class GnirsObservation(odp: ObservationDetailsParameters, ins: GnirsParameters) extends Observation
case class MichelleObservation(odp: ObservationDetailsParameters, ins: MichelleParameters) extends Observation
case class TRecsObservation(odp: ObservationDetailsParameters, ins: TRecsParameters) extends Observation

/**
 * Observations with Altair.
 */
sealed trait AltairObservation extends Observation {
  val alt: AltairParameters
  override val hash = (Hash.calc(odp)*37 + Hash.calc(ins))*37 + Hash.calc(alt)
}
case class NifsObservation(odp: ObservationDetailsParameters, ins: NifsParameters, alt: AltairParameters) extends AltairObservation
case class NiriObservation(odp: ObservationDetailsParameters, ins: NiriParameters, alt: AltairParameters) extends AltairObservation

/**
 * Observations with Gems.
 */
sealed trait GemsObservation extends Observation {
  val gems: GemsParameters
  override val hash = (Hash.calc(odp)*37 + Hash.calc(ins))*37 + Hash.calc(gems)
}
case class GsaoiObservation(odp: ObservationDetailsParameters, ins: GsaoiParameters, gems: GemsParameters) extends GemsObservation

/**
 * Definition of some default observations that can be used in test cases.
 */
object Observation {

  // =============== OBSERVATIONS ====================
  lazy val SpectroscopyObservations = List(
    new ObservationDetailsParameters(
      ObservationDetailsParameters.SPECTROSCOPY,
      ObservationDetailsParameters.S2N,
      10,
      600.0,
      1.0,
      10.0,
      ObservationDetailsParameters.USER_APER,
      0.7,
      3)

  )

  lazy val ImagingObservations = List(
    new ObservationDetailsParameters(
      ObservationDetailsParameters.IMAGING,               // calc mode
      ObservationDetailsParameters.INTTIME,               // calc method
      2,                                                  // exposures
      1800.0,                                             // exposure time
      1.0,                                                // source fraction
      10.0,                                               // SN ratio
      ObservationDetailsParameters.AUTO_APER,             // aperture type
      0.7,                                                // aperture diameter
      3),                                                 // sky aperture diameter

    new ObservationDetailsParameters(
      ObservationDetailsParameters.IMAGING,
      ObservationDetailsParameters.S2N,
      30,
      120.0,
      0.5,
      25.64,
      ObservationDetailsParameters.USER_APER,
      0.7,
      3)
  )

}

package edu.gemini.itc.baseline.util

import edu.gemini.itc.acqcam.AcquisitionCamParameters
import edu.gemini.itc.altair.AltairParameters
import edu.gemini.itc.flamingos2.Flamingos2Parameters
import edu.gemini.itc.gems.GemsParameters
import edu.gemini.itc.gmos.GmosParameters
import edu.gemini.itc.gnirs.GnirsParameters
import edu.gemini.itc.gsaoi.GsaoiParameters
import edu.gemini.itc.michelle.MichelleParameters
import edu.gemini.itc.nici.NiciParameters
import edu.gemini.itc.nifs.NifsParameters
import edu.gemini.itc.niri.NiriParameters
import edu.gemini.itc.parameters.ObservationDetailsParameters
import edu.gemini.itc.shared.ITCParameters
import edu.gemini.itc.trecs.TRecsParameters

// Observation is combination of observation and instrument parameters.
// Important because spectroscopy observations need instrument to be configured accordingly.
sealed trait Observation {
  val odp: ObservationDetailsParameters
  val ins: ITCParameters
  val hash = Hash.calc(odp)*37 + Hash.calc(ins)
}
case class AcqCamObservation(odp: ObservationDetailsParameters, ins: AcquisitionCamParameters) extends Observation
case class GmosObservation(odp: ObservationDetailsParameters, ins: GmosParameters) extends Observation
case class GnirsObservation(odp: ObservationDetailsParameters, ins: GnirsParameters) extends Observation
case class MichelleObservation(odp: ObservationDetailsParameters, ins: MichelleParameters) extends Observation
case class NiciObservation(odp: ObservationDetailsParameters, ins: NiciParameters) extends Observation
case class TRecsObservation(odp: ObservationDetailsParameters, ins: TRecsParameters) extends Observation

sealed trait AltairObservation extends Observation {
  val alt: AltairParameters
  override val hash = (Hash.calc(odp)*37 + Hash.calc(ins))*37 + Hash.calc(alt)
}
case class F2Observation(odp: ObservationDetailsParameters, ins: Flamingos2Parameters, alt: AltairParameters) extends AltairObservation
case class NifsObservation(odp: ObservationDetailsParameters, ins: NifsParameters, alt: AltairParameters) extends AltairObservation
case class NiriObservation(odp: ObservationDetailsParameters, ins: NiriParameters, alt: AltairParameters) extends AltairObservation


sealed trait GemsObservation extends Observation {
  val gems: GemsParameters
  override val hash = (Hash.calc(odp)*37 + Hash.calc(ins))*37 + Hash.calc(gems)
}
case class GsaoiObservation(odp: ObservationDetailsParameters, ins: GsaoiParameters, gems: GemsParameters) extends GemsObservation

/**
 * Created by fnussber on 1/9/15.
 */
object Observation {

  // =============== OBSERVATIONS ====================
  val SpectroscopyObservations = List(
    new ObservationDetailsParameters(
      ObservationDetailsParameters.SPECTROSCOPY,
      ObservationDetailsParameters.INTTIME,
      2,
      1800.0,
      1.0,
      10.0,
      ObservationDetailsParameters.IMAGING,
      ObservationDetailsParameters.AUTO_APER,
      0.7,
      3)
  )

  val ImagingObservations = List(
    new ObservationDetailsParameters(
      ObservationDetailsParameters.IMAGING,
      ObservationDetailsParameters.INTTIME,
      30,
      120.0,
      0.5,
      25.64,
      ObservationDetailsParameters.IMAGING,
      ObservationDetailsParameters.AUTO_APER,
      0.7,
      3)
  )

}

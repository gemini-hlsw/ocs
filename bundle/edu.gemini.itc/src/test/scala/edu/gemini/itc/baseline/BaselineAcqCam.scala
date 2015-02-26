package edu.gemini.itc.baseline

import edu.gemini.itc.acqcam.{AcqCamRecipe, AcquisitionCamParameters}
import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._

/**
 * Acquisition camera baseline test bits and pieces.
 */
object BaselineAcqCam {

  lazy val Observations =
    for {
      odp  <- Observation.ImagingObservations
      conf <- configs()
    } yield AcqCamObservation(odp, conf)

  lazy val Environments =
    for {
      src <- Environment.PointSources
      ocp <- Environment.ObservingConditions
      tep <- Environment.TelescopeConfigurations
      pdp <- Environment.PlottingParameters
    } yield Environment(src, ocp, tep, pdp)

  def executeRecipe(e: Environment, o: AcqCamObservation): Output =
    cookRecipe(w => new AcqCamRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, w))

  private def configs() = List(
    new AcquisitionCamParameters(
      "R",
      AcquisitionCamParameters.NDA)
  )

}

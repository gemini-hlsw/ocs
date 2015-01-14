package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.nici.{NiciParameters, NiciRecipe}


/**
 * NICI baseline test bits and pieces.
 */
object BaselineNici {

  lazy val Observations =
    for {
      odp  <- Observation.ImagingObservations
      conf <- configs()
    } yield NiciObservation(odp, conf)

  lazy val Environments =
    for {
      src <- Environment.NiciSources
      ocp <- Environment.ObservingConditions
      tep <- Environment.TelescopeConfigurations
      pdp <- Environment.PlottingParameters
    } yield Environment(src, ocp, tep, pdp)

  def executeRecipe(e: Environment, o: NiciObservation): Output =
    cookRecipe(w => new NiciRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, w))

  private def configs() = List(
    new NiciParameters(
      "CH4H1S",                 // channel1
      "CH4H1S",                 // channel2
      "open",                   // pupil
      "",                       // instrument mode, unused?
      "h5050")                  // dichroic position
  )

}

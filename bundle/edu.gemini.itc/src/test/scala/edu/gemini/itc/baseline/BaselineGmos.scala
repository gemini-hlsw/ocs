package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gmos.{GmosParameters, GmosRecipe}

/**
 * GMOS baseline test bits and pieces.
 */
object BaselineGmos {

  // Defines a set of valid observations for GMOS
  lazy val Observations =
    specObs() ++
    imgObs()

  lazy val Environments =
    for {
      src <- Environment.GmosSources
      ocp <- Environment.ObservingConditions
      tep <- Environment.TelescopeConfigurations
      pdp <- Environment.PlottingParameters
    } yield Environment(src, ocp, tep, pdp)

  def executeRecipe(e: Environment, o: GmosObservation): Output =
    cookRecipe(w => new GmosRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, e.pdp, w))

  // GMOS imaging observations
  private def imgObs() = for {
    odp <- Observation.ImagingObservations
    ins <- imagingParams()
  } yield GmosObservation(odp, ins)

  private def imagingParams() = List(
    new GmosParameters(
      GmosParameters.I_G0302,
      GmosParameters.NO_DISPERSER,
      GmosParameters.LOW_READ_NOISE,
      GmosParameters.LOW_WELL_DEPTH,
      "4.7",                      // dark current
      "500",                      // wavelength
      GmosParameters.NO_SLIT,
      "1",
      "1",
      "",                         // IFU method
      "0",                        // IFU offset
      "0",
      "0.3",
      "2",                        // HAMAMATSU CCD
      GmosParameters.GMOS_NORTH),

    new GmosParameters(
      GmosParameters.G_G0301,
      GmosParameters.NO_DISPERSER,
      GmosParameters.HIGH_READ_NOISE,
      GmosParameters.HIGH_WELL_DEPTH,
      "4.7",
      "500",
      GmosParameters.NO_SLIT,
      "1",
      "1",
      "",
      "0",
      "0",
      "0.3",
      "2",                        // HAMAMATSU CCD
      GmosParameters.GMOS_SOUTH)
  )

  // GMOS spectroscopy observations
  private def specObs() = for {
    odp <- Observation.SpectroscopyObservations
    ins <- spectroscopyParams()
  } yield GmosObservation(odp, ins)

  private def spectroscopyParams() = List(
    new GmosParameters(
      GmosParameters.G_G0301,
      GmosParameters.R150_G5306,
      GmosParameters.LOW_READ_NOISE,
      GmosParameters.HIGH_WELL_DEPTH,
      "4.7",
      "500",
      GmosParameters.SLIT1_0,
      "1",
      "1",
      "singleIFU",
      "0",
      "0",
      "0.3",
      "0",                        // EEV ED; still supported?
      GmosParameters.GMOS_NORTH),
    new GmosParameters(
      GmosParameters.G_G0301,
      GmosParameters.R150_G5306,
      GmosParameters.LOW_READ_NOISE,
      GmosParameters.HIGH_WELL_DEPTH,
      "4.7",
      "500",
      GmosParameters.IFU,
      "1",
      "1",
      GmosParameters.SINGLE_IFU,
      "0",
      "0",
      "0.3",
      "1",                        // EEV legacy; still supported?
      GmosParameters.GMOS_NORTH),
    new GmosParameters(
      GmosParameters.G_G0301,
      GmosParameters.R150_G5306,
      GmosParameters.LOW_READ_NOISE,
      GmosParameters.HIGH_WELL_DEPTH,
      "4.7",
      "500",
      GmosParameters.IFU,
      "1",
      "1",
      GmosParameters.RADIAL_IFU,
      "0",
      "0",
      "0.3",
      "2",                        // HAMAMATSU CCD
      GmosParameters.GMOS_NORTH)
  )

}


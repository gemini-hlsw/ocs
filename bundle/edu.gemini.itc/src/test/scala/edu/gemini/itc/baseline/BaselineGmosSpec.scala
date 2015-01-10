package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gmos.{GmosParameters, GmosRecipe}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 * A series of tests that compares the generated outputs (HTML pages) with some that have been created and stored
 * previously to starting the current refactoring tasks. These tests are expected to change and evolve over time,
 * in particular as soon as there is a separation of the actual calculation (business logic) and the presentation
 * layer the testing should become simpler.
 *
 * NOTE: If you want to execute these tests in IntelliJ make sure that the resource folder is marked as such.
 */
object BaselineGmosSpec extends Specification with ScalaCheck {

  // Defines a set of valid observations for GMOS
  val Observations =
    // GMOS spectroscopy observations
    (for {
      odp <- Observation.SpectroscopyObservations
      ins <- spectroscopyParams()
    } yield GmosObservation(odp, ins)) ++
    // GMOS imaging observations
    (for {
      odp <- Observation.ImagingObservations
      ins <- imagingParams()
    } yield GmosObservation(odp, ins))

  implicit val arbObservation: Arbitrary[GmosObservation] = Arbitrary { Gen.oneOf(Observations) }

  "GMOS calculations" should {
    "match latest baseline" !
      prop { (e: Environment, o: GmosObservation) =>
        checkAgainstBaseline(Baseline.from(e, o, executeRecipe(e, o)))
      }.set((minTestsOk, 10))
  }

  def executeRecipe(e: Environment, o: GmosObservation): Output =
    cookRecipe(w => new GmosRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, e.pdp, w))


  private def imagingParams() = List(
    new GmosParameters(
      GmosParameters.R600_G5304,
      GmosParameters.NO_DISPERSER,
      GmosParameters.LOW_READ_NOISE,
      GmosParameters.HIGH_WELL_DEPTH,
      "4.7",
      "500",
      GmosParameters.NO_SLIT,
      "1",
      "1",
      "singleIFU",
      "0",
      "0",
      "0.3",
      "2", // HAMAMATSU CCD
      GmosParameters.GMOS_NORTH),

    new GmosParameters(
      GmosParameters.B1200_G5301,
      GmosParameters.NO_DISPERSER,
      GmosParameters.HIGH_READ_NOISE,
      GmosParameters.LOW_WELL_DEPTH,
      "4.7",
      "500",
      GmosParameters.NO_SLIT,
      "1",
      "1",
      "singleIFU",
      "0",
      "0",
      "0.3",
      "2", // HAMAMATSU CCD
      GmosParameters.GMOS_SOUTH)
  )

  private def spectroscopyParams() = List(
    new GmosParameters(
      GmosParameters.R600_G5304,
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
      "2",                      // HAMAMATSU CCD
      "gmosNorth")
  )

}


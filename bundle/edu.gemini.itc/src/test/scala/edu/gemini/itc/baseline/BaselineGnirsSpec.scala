package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gnirs.{GnirsParameters, GnirsRecipe}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 */
object BaselineGnirsSpec extends Specification with ScalaCheck {

  val Observations =
    // GNIRS spectroscopy observations
    for {
      odp <- Observation.SpectroscopyObservations
      ins <- spectroscopyParams()
    } yield GnirsObservation(odp, ins)

  implicit val arbObservation: Arbitrary[GnirsObservation] = Arbitrary { Gen.oneOf(Observations) }

  "GNIRS calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: GnirsObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, executeRecipe(e, o)))
      }.set((minTestsOk, 10))
  }

  def executeRecipe(e: Environment, o: GnirsObservation): Output =
    cookRecipe(w => new GnirsRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, e.pdp, w))



  private def spectroscopyParams() = List(
    new GnirsParameters(
      GnirsParameters.LONG_CAMERA,
      GnirsParameters.G32,
      GnirsParameters.X_DISP_OFF,
      GnirsParameters.LOW_READ_NOISE,
      "4.7",
      "2.4",
      GnirsParameters.SLIT0_1),

    new GnirsParameters(
      GnirsParameters.SHORT_CAMERA,
      GnirsParameters.G32,
      GnirsParameters.X_DISP_OFF,
      GnirsParameters.LOW_READ_NOISE,
      "4.7",
      "2.4",
      GnirsParameters.SLIT0_2),

    new GnirsParameters(
      GnirsParameters.LONG_CAMERA,
      GnirsParameters.G32,
      GnirsParameters.X_DISP_ON,
      GnirsParameters.LOW_READ_NOISE,
      "4.7",
      "2.4",
      GnirsParameters.SLIT0_675)

  )


}

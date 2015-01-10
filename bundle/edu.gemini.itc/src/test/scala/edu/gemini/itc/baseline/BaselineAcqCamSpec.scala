package edu.gemini.itc.baseline

import edu.gemini.itc.acqcam.{AcqCamRecipe, AcquisitionCamParameters}
import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 */
object BaselineAcqCamSpec extends Specification with ScalaCheck {

  val Observations =
    for {
      odp  <- Observation.ImagingObservations
      conf <- configs()
    } yield AcqCamObservation(odp, conf)

  implicit val arbObservation: Arbitrary[AcqCamObservation] = Arbitrary { Gen.oneOf(Observations) }

  "Acquisition Camera calculations" should {
    "match latest baseline" !
      prop { (e: Environment, o: AcqCamObservation) =>
        checkAgainstBaseline(Baseline.from(e, o, executeRecipe(e, o)))
      }.set((minTestsOk, 10))
  }

  def executeRecipe(e: Environment, o: AcqCamObservation): Output =
    cookRecipe(w => new AcqCamRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, w))

  private def configs() = List(
    new AcquisitionCamParameters(
      "R",
      AcquisitionCamParameters.NDA)
  )

}

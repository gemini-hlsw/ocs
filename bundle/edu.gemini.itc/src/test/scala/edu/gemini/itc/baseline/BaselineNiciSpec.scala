package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.nici.{NiciParameters, NiciRecipe}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification


/**
 * Nici test cases.
 */
object BaselineNiciSpec extends Specification with ScalaCheck {

  val Observations =
    for {
      odp  <- Observation.ImagingObservations
      conf <- configs()
    } yield NiciObservation(odp, conf)

  implicit val arbObservation: Arbitrary[NiciObservation] = Arbitrary { Gen.oneOf(Observations) }

  "NICI calculations" should {
    "match latest baseline" !
      prop { (e: Environment, o: NiciObservation) =>
        checkAgainstBaseline(Baseline.from(e, o, executeRecipe(e, o)))
      }.set((minTestsOk, 10))
  }

  def executeRecipe(e: Environment, o: NiciObservation): Output =
    cookRecipe(w => new NiciRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, w))

  private def configs() = List(
    new NiciParameters(
      "H",                    // channel1
      "ndfilt_clear",         // channel2
      "80",                   // pupil
      "",                     // instrument mode, unused?
      "h5050")                // dichroic position
  )

}

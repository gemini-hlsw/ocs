package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.niri.{NiriParameters, NiriRecipe}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 */
object BaselineNiriSpec extends Specification with ScalaCheck {

  val Observations =
    for {
      odp  <- Observation.ImagingObservations
      alt  <- Environment.AltairConfigurations
      conf <- configs()
    } yield NiriObservation(odp, conf, alt)

  implicit val arbObservation: Arbitrary[NiriObservation] = Arbitrary { Gen.oneOf(Observations) }

  "NIRI calculations" should {
    "match latest baseline" !
      prop { (e: Environment, o: NiriObservation) =>
        checkAgainstBaseline(Baseline.from(e, o, executeRecipe(e, o)))
      }.set((minTestsOk, 10))
  }

  def executeRecipe(e: Environment, o: NiriObservation): Output =
    cookRecipe(w => new NiriRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, o.alt, e.pdp, w))

  private def configs() = List(
    new NiriParameters("J",
      "none",
      NiriParameters.F6,
      NiriParameters.LOW_READ_NOISE,
      NiriParameters.HIGH_WELL_DEPTH,
      NiriParameters.NO_SLIT)
  )

}

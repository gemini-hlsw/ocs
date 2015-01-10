package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.flamingos2.{Flamingos2Parameters, Flamingos2Recipe}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 */
object BaselineF2Spec extends Specification with ScalaCheck {

  val Observations =
    for {
      odp  <- Observation.ImagingObservations
      alt  <- Environment.AltairConfigurations
      conf <- configs()
    } yield F2Observation(odp, conf, alt)

  implicit val arbObservation: Arbitrary[F2Observation] = Arbitrary { Gen.oneOf(Observations) }

  "Flamingos2 calculations" should {
    "match latest baseline" !
      prop { (e: Environment, o: F2Observation) =>
        checkAgainstBaseline(Baseline.from(e, o, executeRecipe(e, o)))
      }.set((minTestsOk, 10))
  }

  def executeRecipe(e: Environment, o: F2Observation): Output =
    cookRecipe(w => new Flamingos2Recipe(e.src, o.odp, e.ocp, o.ins, e.tep, o.alt, e.pdp, w))

  private def configs() = List(
    new Flamingos2Parameters("H_G0803", Flamingos2Parameters.NOGRISM, "1", "medNoise")
  )

}

package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.nifs.{NifsParameters, NifsRecipe}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 */
object BaselineNifsSpec extends Specification with ScalaCheck {

  val Observations =
    for {
      odp  <- Observation.ImagingObservations
      alt  <- Environment.AltairConfigurations
      conf <- configs()
    } yield NifsObservation(odp, conf, alt)

  implicit val arbObservation: Arbitrary[NifsObservation] = Arbitrary { Gen.oneOf(Observations) }

  "NIFS calculations" should {
    "match latest baseline" !
      prop { (e: Environment, o: NifsObservation) =>
        checkAgainstBaseline(Baseline.from(e, o, executeRecipe(e, o)))
      }.set((minTestsOk, 10))
  }

  def executeRecipe(e: Environment, o: NifsObservation): Output =
    cookRecipe(w => new NifsRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, o.alt, e.pdp, w))

  private def configs() = List(
    new NifsParameters(
      NifsParameters.HK_G0603,
      NifsParameters.K_G5605,
      NifsParameters.LOW_READ_NOISE,
      "2.1",
      "2.1",
      NifsParameters.IFU,
      "singleIFU",
      "0",
      "0",
      "0.3",
      "nifsNorth")
  )

}

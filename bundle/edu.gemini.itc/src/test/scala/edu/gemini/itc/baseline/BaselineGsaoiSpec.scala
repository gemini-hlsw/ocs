package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gems.GemsParameters
import edu.gemini.itc.gsaoi.{GsaoiParameters, GsaoiRecipe}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 * GSAOI test cases.
 */
object BaselineGsaoiSpec extends Specification with ScalaCheck  {

  val Observations =
    for {
      odp  <- Observation.SpectroscopyObservations
      ins  <- config()
      gems <- gems()
    } yield GsaoiObservation(odp, ins, gems)

  implicit val arbObservation: Arbitrary[GsaoiObservation] = Arbitrary { Gen.oneOf(Observations) }

  "GSAOI calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: GsaoiObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, executeRecipe(e, o)))
      }.set((minTestsOk, 10))
  }

  def executeRecipe(e: Environment, o: GsaoiObservation): Output =
    cookRecipe(w => new GsaoiRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, o.gems, w))

  private def gems() = List(
    new GemsParameters(
      0.3,
      "K"
    )
  )

  private def config() = List(
    new GsaoiParameters(
      "Z_G1101",                                    //String Filter,
      GsaoiParameters.INSTRUMENT_CAMERA,            //String camera,
      GsaoiParameters.BRIGHT_OBJECTS_READ_MODE      //String read mode,
    )

  )


}

package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.michelle.{MichelleParameters, MichelleRecipe}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 * Michelle test cases.
 * Michelle is not in use anymore but science wants to keep the ITC functionality alive as a reference.
 */
object BaselineMichelleSpec extends Specification with ScalaCheck  {

  val Observations =
    for {
      odp <- Observation.SpectroscopyObservations
      ins <- config()
    } yield MichelleObservation(odp, ins)

  implicit val arbObservation: Arbitrary[MichelleObservation] = Arbitrary { Gen.oneOf(Observations) }

  "Michelle calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: MichelleObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, executeRecipe(e, o)))
      }.set((minTestsOk, 10))
  }

  def executeRecipe(e: Environment, o: MichelleObservation): Output =
    cookRecipe(w => new MichelleRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, e.pdp, w))


  private def config() = List(
    // Michelle spectroscopy
    new MichelleParameters(
      "none",                             //String Filter,
      MichelleParameters.LOW_N,           //String grating,
      MichelleParameters.HIGH_READ_NOISE, //String readNoise,
      MichelleParameters.HIGH_WELL_DEPTH, //String wellDepth,
      "NOT_USED?",                        //String darkCurrent, TODO UNUSED?
      "777",                              //String instrumentCentralWavelength,
      MichelleParameters.SLIT0_19,        //String FP_Mask,
      "1",                                //String spatBinning,
      "1",                                //String specBinning
      MichelleParameters.DISABLED         //String polarimetry (enabled only allowed if imaging)
    )

  )


}

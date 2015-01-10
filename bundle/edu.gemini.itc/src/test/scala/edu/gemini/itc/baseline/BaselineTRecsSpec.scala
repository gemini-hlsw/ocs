package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.trecs.{TRecsParameters, TRecsRecipe}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 * TRecs test cases.
 * TRecs is not in use anymore but science wants to keep the ITC functionality alive as a reference.
 */
object BaselineTRecsSpec extends Specification with ScalaCheck  {

  val Observations =
    for {
      odp <- Observation.SpectroscopyObservations
      ins <- config()
    } yield TRecsObservation(odp, ins)

  implicit val arbObservation: Arbitrary[TRecsObservation] = Arbitrary { Gen.oneOf(Observations) }

  "TRecs calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: TRecsObservation) =>

          // special rule for TRecs: for mid-IR SB percentile must equal WV percentile!
          // this rules seems to be enforced at all times for TRecs
          // TODO: use whenever?
          if (isValidForTRecs(e)) {
            checkAgainstBaseline(Baseline.from(e, o, executeRecipe(e, o)))
          } else {
            true
          }

      }.set((minTestsOk, 10))
  }

  def isValidForTRecs(e: Environment) =
    e.ocp.getSkyBackground == e.ocp.getSkyTransparencyWater

  def executeRecipe(e: Environment, o: TRecsObservation): Output =
    cookRecipe(w => new TRecsRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, e.pdp, w))


  private def config() = List(
    new TRecsParameters(
      "none",                             //String Filter,
      "KBr",                              //String Instrumentwindow
      "HiRes-10",                         //String grating, ("none") for imaging
      TRecsParameters.HIGH_READ_NOISE,    //String readNoise,
      TRecsParameters.HIGH_WELL_DEPTH,    //String wellDepth,
      "NOT_USED?",                        //String darkCurrent, TODO UNUSED?
      "777",                              //String instrumentCentralWavelength,
      TRecsParameters.SLIT0_21,           //String FP_Mask,
      "1",                                //String spatBinning,
      "1"                                 //String specBinning
    )

  )


}

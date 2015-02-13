package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 * Spec which compares a limited amount of random ITC "recipe" executions with the expected outcome.
 * Test are executed by using a hash value of the input values (environment, observation) as a key in a map
 * that contains hash values of the expected output of the recipe execution (currently a string). This baseline
 * map is stored as a resource file and needs to be updated whenever there are changes to the code that change
 * the outputs. See [[BaselineTest]] for details.
 */
object BaselineAllSpec extends Specification with ScalaCheck {

  // default number of tests is 100, that takes a bit too long
  private val minTestsCnt = 10

  // === ACQUISITION CAMERA
  {
    implicit val arbO: Arbitrary[AcqCamObservation] = Arbitrary { Gen.oneOf(BaselineAcqCam.Observations) }
    implicit val arbE: Arbitrary[Environment]       = Arbitrary { Gen.oneOf(BaselineAcqCam.Environments) }

    "Acquisition Camera calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: AcqCamObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, BaselineAcqCam.executeRecipe(e, o)))
        }.set((minTestsOk, minTestsCnt))
    }
  }

  // === F2
  {
    implicit val arbO: Arbitrary[F2Observation] = Arbitrary { Gen.oneOf(BaselineF2.Observations) }
    implicit val arbE: Arbitrary[Environment]   = Arbitrary { Gen.oneOf(BaselineF2.Environments) }

    "Flamingos2 calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: F2Observation) =>
          checkAgainstBaseline(Baseline.from(e, o, BaselineF2.executeRecipe(e, o)))
        }.set((minTestsOk, minTestsCnt))
    }
  }

  // === GMOS
  {
    implicit val arbO: Arbitrary[GmosObservation] = Arbitrary { Gen.oneOf(BaselineGmos.Observations) }
    implicit val arbEs: Arbitrary[Environment]    = Arbitrary { Gen.oneOf(BaselineGmos.Environments) }

    "GMOS calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: GmosObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, BaselineGmos.executeRecipe(e, o)))
        }.set((minTestsOk, minTestsCnt))
    }
  }

  // === GNIRS
  {
    implicit val arbO: Arbitrary[GnirsObservation] = Arbitrary { Gen.oneOf(BaselineGnirs.Observations) }
    implicit val arbE: Arbitrary[Environment]      = Arbitrary { Gen.oneOf(BaselineGnirs.Environments) }

    "GNIRS calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: GnirsObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, BaselineGnirs.executeRecipe(e, o)))
        }.set((minTestsOk, minTestsCnt))
    }
  }

  // === GSAOI
  {
    implicit val arbO: Arbitrary[GsaoiObservation] = Arbitrary { Gen.oneOf(BaselineGsaoi.Observations) }
    implicit val arbE: Arbitrary[Environment]      = Arbitrary { Gen.oneOf(BaselineGsaoi.Environments) }

    "GSAOI calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: GsaoiObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, BaselineGsaoi.executeRecipe(e, o)))
        }.set((minTestsOk, minTestsCnt))
    }
  }

  // === Michelle
  {
    implicit val arbO: Arbitrary[MichelleObservation] = Arbitrary { Gen.oneOf(BaselineMichelle.Observations) }
    implicit val arbE: Arbitrary[Environment]         = Arbitrary { Gen.oneOf(BaselineMichelle.Environments) }

    "Michelle calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: MichelleObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, BaselineMichelle.executeRecipe(e, o)))
        }.set((minTestsOk, minTestsCnt))
    }
  }

  // === NIFS
  {
    implicit val arbO: Arbitrary[NifsObservation] = Arbitrary { Gen.oneOf(BaselineNifs.Observations) }
    implicit val arbE: Arbitrary[Environment]     = Arbitrary { Gen.oneOf(BaselineNifs.Environments) }

    "NIFS calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: NifsObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, BaselineNifs.executeRecipe(e, o)))
        }.set((minTestsOk, minTestsCnt))
    }
  }

  // === NIRI
  {
    implicit val arbO: Arbitrary[NiriObservation] = Arbitrary { Gen.oneOf(BaselineNiri.Observations) }
    implicit val arbE: Arbitrary[Environment]     = Arbitrary { Gen.oneOf(BaselineNiri.Environments) }

    "NIRI calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: NiriObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, BaselineNiri.executeRecipe(e, o)))
        }.set((minTestsOk, minTestsCnt))
    }
  }

  // === TRecs
  {
    implicit val arbO: Arbitrary[TRecsObservation] = Arbitrary { Gen.oneOf(BaselineTRecs.Observations) }
    implicit val arbE: Arbitrary[Environment]      = Arbitrary { Gen.oneOf(BaselineTRecs.Environments) }

    "TRecs calculations" should {
      "match latest baseline" !
        prop { (e: Environment, o: TRecsObservation) =>
          checkAgainstBaseline(Baseline.from(e, o, BaselineTRecs.executeRecipe(e, o)))
        }.set((minTestsOk, minTestsCnt))
    }
  }
}

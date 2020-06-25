package edu.gemini.itc.web.baseline

import edu.gemini.itc.baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared._
import edu.gemini.itc.web.baseline.Baseline._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalacheck.ScalaCheckParameters

/**
 * Spec which compares a limited amount of random ITC "recipe" executions with the expected outcome.
 * Test are executed by using a hash value generated from the fixture as a key in a map
 * that contains hash values of the expected output of the recipe execution (currently a string). This baseline
 * map is stored as a resource file and needs to be updated whenever there are changes to the code that change
 * the outputs. See [[BaselineTest]] for details.
 */
object BaselineAllSpec extends Specification with ScalaCheck with ScalaCheckParameters {

  // default number of tests is 100, that takes a bit too long
  private val minTestsCnt = 10

  // === ACQUISITION CAMERA
  {
    implicit val arbFixture: Arbitrary[Fixture[AcquisitionCamParameters]] = Arbitrary { Gen.oneOf(BaselineAcqCam.Fixtures) }

    "Acquisition Camera calculations" should {
      "match latest baseline" !
        prop { f: Fixture[AcquisitionCamParameters] =>
          checkAgainstBaseline(Baseline.from(f, executeAcqCamRecipe(f)))
        }.set(defaultParameters.minTestsOk, minTestsCnt)
    }
  }

  // === F2
  {
    implicit val arbFixture: Arbitrary[Fixture[Flamingos2Parameters]] = Arbitrary { Gen.oneOf(BaselineF2.Fixtures) }

    "Flamingos2 calculations" should {
      "match latest baseline" !
        prop { f: Fixture[Flamingos2Parameters] =>
          checkAgainstBaseline(Baseline.from(f, executeF2Recipe(f)))
        }.set(defaultParameters.minTestsOk, minTestsCnt)
    }
  }

  // === GMOS
  {
    implicit val arbFixture: Arbitrary[Fixture[GmosParameters]] = Arbitrary { Gen.oneOf(BaselineGmos.Fixtures) }

    "GMOS calculations" should {
      "match latest baseline" !
        prop { f: Fixture[GmosParameters] =>
          checkAgainstBaseline(Baseline.from(f, executeGmosRecipe(f)))
        }.set(defaultParameters.minTestsOk, minTestsCnt)
    }
  }

  // === GNIRS
  {
    implicit val arbFixture: Arbitrary[Fixture[GnirsParameters]] = Arbitrary { Gen.oneOf(BaselineGnirs.Fixtures) }

    "GNIRS calculations" should {
      "match latest baseline" !
        prop { f: Fixture[GnirsParameters] =>
          checkAgainstBaseline(Baseline.from(f, executeGnirsRecipe(f)))
        }.set(defaultParameters.minTestsOk, minTestsCnt)
    }
  }

  // === GSAOI
  {
    implicit val arbFixture: Arbitrary[Fixture[GsaoiParameters]] = Arbitrary { Gen.oneOf(BaselineGsaoi.Fixtures) }

    "GSAOI calculations" should {
      "match latest baseline" !
        prop { f: Fixture[GsaoiParameters] =>
          checkAgainstBaseline(Baseline.from(f, executeGsaoiRecipe(f)))
        }.set(defaultParameters.minTestsOk, minTestsCnt)
    }
  }

  // === Michelle
  {
    implicit val arbFixture: Arbitrary[Fixture[MichelleParameters]] = Arbitrary { Gen.oneOf(BaselineMichelle.Fixtures) }

    "Michelle calculations" should {
      "match latest baseline" !
        prop { f: Fixture[MichelleParameters] =>
          checkAgainstBaseline(Baseline.from(f, executeMichelleRecipe(f)))
        }.set(defaultParameters.minTestsOk, minTestsCnt)
    }
  }

  // === NIFS
  {
    implicit val arbFixture: Arbitrary[Fixture[NifsParameters]] = Arbitrary { Gen.oneOf(BaselineNifs.Fixtures) }

    "NIFS calculations" should {
      "match latest baseline" !
        prop { f: Fixture[NifsParameters] =>
          checkAgainstBaseline(Baseline.from(f, executeNifsRecipe(f)))
        }.set(defaultParameters.minTestsOk, minTestsCnt)
    }
  }

  // === NIRI
  {
    implicit val arbFixture: Arbitrary[Fixture[NiriParameters]] = Arbitrary { Gen.oneOf(BaselineNiri.Fixtures) }

    "NIRI calculations" should {
      "match latest baseline" !
        prop { f: Fixture[NiriParameters] =>
          checkAgainstBaseline(Baseline.from(f, executeNiriRecipe(f)))
        }.set(defaultParameters.minTestsOk, minTestsCnt)
    }
  }

  // === TRecs
  {
    implicit val arbFixture: Arbitrary[Fixture[TRecsParameters]] = Arbitrary { Gen.oneOf(BaselineTRecs.Fixtures) }

    "TRecs calculations" should {
      "match latest baseline" !
        prop { f: Fixture[TRecsParameters] =>
          checkAgainstBaseline(Baseline.from(f, executeTrecsRecipe(f)))
        }.set(defaultParameters.minTestsOk, minTestsCnt)
    }
  }
}

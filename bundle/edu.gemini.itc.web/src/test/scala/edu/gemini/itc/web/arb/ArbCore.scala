package edu.gemini.itc.web.arb

import edu.gemini.spModel.core._
import org.scalacheck._

trait ArbCore extends Arbitraries {

  val genMagnitudeSystem: Gen[MagnitudeSystem] =
    Gen.oneOf(MagnitudeSystem.all)

  val genSurfaceBrightness: Gen[SurfaceBrightness] =
    Gen.oneOf(SurfaceBrightness.all)

  val genBrightnessUnit: Gen[BrightnessUnit] =
    Gen.oneOf(genMagnitudeSystem, genSurfaceBrightness)

  implicit val arbBrightnessUnit: Arbitrary[BrightnessUnit] =
    Arbitrary(genBrightnessUnit)

}

object core extends ArbCore
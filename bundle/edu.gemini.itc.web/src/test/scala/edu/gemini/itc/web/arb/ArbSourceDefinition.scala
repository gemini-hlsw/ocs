package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import edu.gemini.spModel.core._
import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ArbSourceDefinition {
  import core._

  val genSourceDefinition: Gen[SourceDefinition] =
    for {
      p <- arbitrary[SpatialProfile]
      d <- arbitrary[SpectralDistribution]
      n <- arbitrary[Double]
      u <- arbitrary[BrightnessUnit]
      b <- arbitrary[MagnitudeBand]
      z <- arbitrary[Redshift]
    } yield SourceDefinition(p, d, n, u, b, z)

  implicit val arbSourceDefinition: Arbitrary[SourceDefinition] =
    Arbitrary(genSourceDefinition)

}

object sourcedefinition extends ArbSourceDefinition
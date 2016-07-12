package edu.gemini.spModel.obs

import edu.gemini.spModel.obs.SchedulingBlock.Duration
import edu.gemini.spModel.obs.SchedulingBlock.Duration._

import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

trait Arbitraries {

  implicit val arbExplicit: Arbitrary[Explicit] =
    Arbitrary(arbitrary[Long].map(n => Explicit(n.abs)))

  implicit val arbComputed: Arbitrary[Computed] =
    Arbitrary(arbitrary[Long].map(n => Computed(n.abs)))

  implicit val arbDuration: Arbitrary[Duration] =
    Arbitrary(oneOf(arbitrary[Explicit], arbitrary[Computed], Gen.const(Unstated)))

  implicit val arbSchedulingBlock: Arbitrary[SchedulingBlock] =
    Arbitrary {
      for {
        start    <- arbitrary[Long]
        duration <- arbitrary[Duration]
      } yield SchedulingBlock(start, duration)
    }

}

package edu.gemini.ags.servlet.arb

import edu.gemini.ags.servlet.AgsResult
import edu.gemini.spModel.core._

import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ArbAgsResult {

  import siderealtarget._

  implicit val arbAgsResult: Arbitrary[AgsResult] =
    Arbitrary {
      for {
        d <- Gen.choose(0, 359)
        t <- arbitrary[SiderealTarget]
      } yield AgsResult(Angle.fromDegrees(d.toDouble), t)
    }

}

object agsresult extends ArbAgsResult

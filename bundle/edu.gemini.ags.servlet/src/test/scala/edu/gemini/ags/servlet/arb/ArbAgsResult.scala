package edu.gemini.ags.servlet.arb

import edu.gemini.ags.servlet.AgsResult
import edu.gemini.spModel.core._

import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ArbAgsResult {

  import siderealtarget._

  implicit val arbAgsResult: Arbitrary[AgsResult] =
    Arbitrary {
      arbitrary[Option[SiderealTarget]].map(AgsResult.apply)
    }

}

object agsresult extends ArbAgsResult

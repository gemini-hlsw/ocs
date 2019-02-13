package edu.gemini.spdb.rapidtoo.www


import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.spdb.rapidtoo.www.HttpTargetSpec._

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import java.time.Duration
import javax.servlet.http.HttpServletRequest


object HttpUpdateSpec extends Specification with ScalaCheck with Arbitraries with edu.gemini.spModel.core.Arbitraries {

  // a valid request
  val req = MockRequest(
    "prog"     -> "GS-3000-TOO-1",
    "posangle" -> "42.0",
    "exptime"  -> "6",
    "note"     -> "hello",
    "ready"    -> "true",
    "email"    -> "bob@burger.com",
    "password" -> "foo",
    "obsnum"   -> "1",
    "target"   -> "canopus",
    "ra"       -> "06:23:57.110",
    "dec"      -> "-52:41:44.39"
  )

  val minSec = HttpTooUpdate.MIN_EXPOSURE_TIME.getSeconds.toInt
  val maxSec = HttpTooUpdate.MAX_EXPOSURE_TIME.getSeconds.toInt

  "HttpTooUpdate" should {

    def mustFail(req: HttpServletRequest) =
      new HttpTooUpdate(req) must throwA[BadRequestException]

    "exptime: fail if out of range" ! forAll(Gen.oneOf(Gen.choose(Int.MinValue, minSec-1), Gen.choose(maxSec+1, Int.MaxValue))) { (e: Int) =>
      mustFail(req.modifiedWith("exptime" -> e.toString))
    }

    "exptime: succeed if in range" ! forAll(Gen.choose(minSec, maxSec)) { (e: Int) =>
       (new HttpTooUpdate(req.modifiedWith("exptime" -> e.toString))).getExposureTime() must_== ImOption.apply(Duration.ofSeconds(e))
    }

    "exptime: fail if not parseable" in {
      mustFail(req.modifiedWith("exptime" -> "1.4"))
    }

    "exptime: succeed if not specified" in {
      (new HttpTooUpdate(req.without("exptime"))).getExposureTime() must_== ImOption.empty()
    }
  }
}

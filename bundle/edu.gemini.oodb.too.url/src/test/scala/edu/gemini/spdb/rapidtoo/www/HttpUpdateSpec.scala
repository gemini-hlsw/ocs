package edu.gemini.spdb.rapidtoo.www


import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.spdb.rapidtoo.www.HttpTargetSpec._

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

  "HttpTooUpdate" should {

    def mustFail(req: HttpServletRequest) =
      new HttpTooUpdate(req) must throwA[BadRequestException]

    "exptime: fail if out of range" ! forAll { (e: Int) =>
      val req2 = req.modifiedWith("exptime" -> e.toString)
      if (e < 1 || e > 300) mustFail(req2)
      else (new HttpTooUpdate(req2)).getExposureTime() must_== ImOption.apply(Duration.ofSeconds(e))
    }

    "exptime: fail if not parseable" in {
      mustFail(req.modifiedWith("exptime" -> "1.4"))
    }

    "exptime: fail if too small" in {
      mustFail(req.modifiedWith("exptime" -> "0"))
    }

    "exptime: fail if too big" in {
      mustFail(req.modifiedWith("exptime" -> "301"))
    }

    "exptime: succeed if 1 <= exptime <= 300" in {
       (new HttpTooUpdate(req.modifiedWith("exptime" -> "6"))).getExposureTime() must_== ImOption.apply(Duration.ofSeconds(6L))
    }

    "exptime: succeed if not specified" in {
      (new HttpTooUpdate(req.without("exptime"))).getExposureTime() must_== ImOption.empty()
    }
  }
}

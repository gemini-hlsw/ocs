package edu.gemini.qpt.shared.sp

import edu.gemini.qpt.shared.util.ObsBuilder
import edu.gemini.spModel.gemini.gmos.GmosNorthType.{DisperserNorth, FilterNorth}
import edu.gemini.spModel.gemini.nici.NICIParams
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 * Test some basic Obs functionality.
 */
object ObsSpec extends Specification with ScalaCheck {

  "Obs" should {
    "store all GN dispersers and filters and NICI FP Masks (REL-2059)" ! {

      val options =
        DisperserNorth.values() ++
          FilterNorth.values() ++
          NICIParams.FocalPlaneMask.values()

      forAll (Gen.someOf(options)) { o =>
        val optionsInObs = ObsBuilder(options = o.toSet).apply.getOptions
        // check that Obs objects don't loose any options when storing them in sorted tree set
        optionsInObs.size == o.size
      }
    }
  }
}

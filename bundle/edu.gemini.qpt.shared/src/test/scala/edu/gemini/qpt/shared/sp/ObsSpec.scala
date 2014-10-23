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

  /**
   * NOTE: Instead of allowing Enums which are not known to HeterogeneousEnumComparator in Obs and for which no class
   * weight and therefore no sort order is defined we should probably disallow this and assert the existence of
   * a defined weight for all used enum options in HeterogeneousEnumComparator.classWeight(). However right now
   * I am not sure if we really never have this case and therefore am reluctant to assert this, so for now
   * I am going to make sure that options in Obs can be of any enum type.
   */
  "Obs" should {
    "store options of unweighted/unknown enum types (REL-2059)" ! {

      val options =
         DisperserNorth.values() ++
           SillyEnums.UnweightedEnum1.values() ++
           SillyEnums.UnweightedEnum2.values()

      forAll (Gen.someOf(options)) { o =>
        val optionsInObs = ObsBuilder(options = o.toSet).apply.getOptions
        // check that Obs objects don't loose any options when storing them in sorted tree set
        optionsInObs.size == o.size
      }
    }
  }

}

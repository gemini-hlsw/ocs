package edu.gemini.qv.plugin.filter.core

import edu.gemini.qpt.shared.util.ObsBuilder
import edu.gemini.spModel.core.{RightAscension, Arbitraries}
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth
import edu.gemini.spModel.gemini.gmos.InstGmosNorth
import org.scalacheck.{Prop, Gen}
import org.scalacheck.Prop._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/**
 * Tests for base filter functionality.
 */
class FilterSpec extends Specification with ScalaCheck with Arbitraries {

  "Configuration filter" should {

    "filter GN dispersers" ! {

      val dispersers = Gen.someOf(DisperserNorth.values())
      forAll (dispersers, dispersers) { (obsDisp, filtDisp) =>
        val o = ObsBuilder(instrument = Array(InstGmosNorth.SP_TYPE), options = obsDisp.toSet).apply
        val f = Filter.GmosN.Dispersers(filtDisp.toSet)
        f.predicate(o) == (obsDisp.intersect(filtDisp).length > 0)
      }
    }
  }

  "RA filter" should {

    "filter right ascension for minRa <= maxRa" ! {

      forAll { (ra: RightAscension, minRa: RightAscension, maxRa: RightAscension) =>
        val minVal = minRa.toAngle.toDegrees / 15.0
        val maxVal = maxRa.toAngle.toDegrees / 15.0
        (minVal <= maxVal) ==> Prop {
          val raVal = ra.toAngle.toDegrees / 15.0
          val o = ObsBuilder().setRa(raVal).apply
          val f = Filter.RA(minVal, maxVal)
          f.predicate(o) == (minVal <= raVal && raVal < maxVal)
        }
      }
    }

    "filter right ascension for minRa > maxRa wrapped around 24hrs (REL-2015)" ! {

      forAll { (ra: RightAscension, minRa: RightAscension, maxRa: RightAscension) =>
        val minVal = minRa.toAngle.toDegrees / 15.0
        val maxVal = maxRa.toAngle.toDegrees / 15.0
        (minVal > maxVal) ==> Prop {
          val raVal = ra.toAngle.toDegrees / 15.0
          val o = ObsBuilder().setRa(raVal).apply
          val f = Filter.RA(minVal, maxVal)
          // e.g [18..5] turns into [0..5] || [18..]
          f.predicate(o) == ((0 <= raVal && raVal < maxVal) || minVal <= raVal)
        }
      }
    }
  }


}

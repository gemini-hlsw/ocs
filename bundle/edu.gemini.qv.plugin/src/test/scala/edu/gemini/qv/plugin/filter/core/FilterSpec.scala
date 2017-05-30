package edu.gemini.qv.plugin.filter.core

import edu.gemini.qpt.shared.util.ObsBuilder
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.data.{DataSource, ObservationProvider}
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth
import edu.gemini.spModel.gemini.gmos.InstGmosNorth
import org.scalacheck.Prop._
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz.Scalaz._

/**
 * Tests for base filter functionality.
 */
class FilterSpec extends Specification with ScalaCheck with Arbitraries {

  // An empty context for testing.
  val ctx = QvContext(new Peer("", 0, Site.GN), DataSource.empty(Site.GN), ObservationProvider.empty)

  "Configuration filter" should {

    "filter GN dispersers" ! {

      val dispersers = Gen.someOf(DisperserNorth.values())
      forAll (dispersers, dispersers) { (obsDisp, filtDisp) =>
        val o = ObsBuilder(instrument = Array(InstGmosNorth.SP_TYPE), options = obsDisp.toSet).apply
        val f = Filter.GmosN.Dispersers(filtDisp.toSet)
        f.predicate(o, ctx) == obsDisp.intersect(filtDisp).nonEmpty
      }
    }
  }

  "RA filter" should {

    "filter right ascension for minRa <= maxRa" ! {

      forAll { (ra: RightAscension, minRa: RightAscension, maxRa: RightAscension) =>
        (minRa <= maxRa) ==> Prop {
          val o = ObsBuilder().setCoordinates(coordinatesFromRa(ra)).apply
          val f = Filter.RA(minRa, maxRa)
          f.predicate(o, ctx) == (minRa <= ra && ra < maxRa)
        }
      }
    }

    "filter right ascension for minRa > maxRa wrapped around 24hrs (REL-2015)" ! {

      forAll { (ra: RightAscension, minRa: RightAscension, maxRa: RightAscension) =>
        (minRa > maxRa) ==> Prop {
          val o = ObsBuilder().setCoordinates(coordinatesFromRa(ra)).apply
          val f = Filter.RA(minRa, maxRa)
          // e.g [18..5] turns into [0..5] || [18..]
          f.predicate(o, ctx) == ((RightAscension.zero <= ra && ra < maxRa) || minRa <= ra)
        }
      }
    }
  }


  // NOTE: Targets with coordinates (ra=0,dec=0) are treated in special ways. For the tests above to
  // work as expected we set dec to a value != 0 in order to be able to cover all possible RA values
  // without running into the special case of 'dummy' targets.
  def coordinatesFromRa(ra: RightAscension) =
    Coordinates(ra, Declination.fromAngle(Angle.fromDegrees(1.0)).get)
}

package edu.gemini.ags.gems

import edu.gemini.ags.TargetsHelper
import edu.gemini.catalog.api._
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.scalacheck.Prop._

import scalaz._
import Scalaz._

class CatalogSearchCriterionSpec extends Specification with Arbitraries with ScalaCheck with TargetsHelper {
  val mc = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(10.0), Some(SaturationConstraint(2.0)))

  "CatalogSearchCriterion" should {
    "not adjust limits if posAngle defined" in {
      forAll { (offset: Offset) =>
        val rc = RadiusConstraint.between(Angle.fromArcmin(1.0), Angle.fromArcmin(2.0))
        val criterion = CatalogSearchCriterion("name", rc, MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(10.0), None), offset.some, Angle.fromDegrees(45).some)
        criterion.adjustedLimits should beEqualTo(rc)
      }
    }
    "adjust limits if posAngle is not defined" in {
      forAll { (offset: Offset) =>
        (offset =/= Offset.zero) ==> {
          val rc = RadiusConstraint.between(Angle.fromArcmin(1.0), Angle.fromArcmin(2.0))
          val criterion = CatalogSearchCriterion("name", rc, MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(10.0), None), offset.some, None)
          criterion.adjustedLimits should not be equalTo(rc)
          criterion.adjustedLimits.minLimit.toDegrees should beGreaterThanOrEqualTo(0.0)
          criterion.adjustedLimits.maxLimit.toDegrees should beGreaterThanOrEqualTo(0.0)
          criterion.adjustedLimits.maxLimit should beGreaterThanOrEqualTo(criterion.adjustedLimits.minLimit)
        }
      }
    }
    "support simple searches" in {
      val radiusLimits = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val criterion = CatalogSearchCriterion("test", radiusLimits, mc, None, None)
      val baseRA = Angle.fromDegrees(10.0)
      val baseDec = Angle.fromDegrees(15.0)
      val base = Coordinates(RightAscension.fromAngle(baseRA), Declination.fromAngle(baseDec).getOrElse(Declination.zero))
      val matcher = criterion.matcher(base)

      // base pos is not in radius limit
      matcher.matches(target("testObj", base, List(new Magnitude(3.0, MagnitudeBand.J)))) should beFalse
      val pos = Coordinates(RightAscension.fromAngle(baseRA), Declination.fromAngle(baseDec + Angle.fromArcmin(3.0)).getOrElse(Declination.zero))

      // Should be in mag and radius limit
      matcher.matches(target("testObj", pos, List(new Magnitude(3.0, MagnitudeBand.J)))) should beTrue
      // mag out of range
      matcher.matches(target("testObj", pos, List(new Magnitude(11.0, MagnitudeBand.J)))) should beFalse
    }
    "support search with offset" in {
      val radiusLimits = RadiusConstraint.between(Angle.fromArcmin(1.0), Angle.fromArcmin(10.0))
      val offset = Offset(1.arcmins[OffsetP], 1.arcmins[OffsetQ]).some
      val criterion = CatalogSearchCriterion("test", radiusLimits, mc, offset, None)
      val baseRA = Angle.fromDegrees(10.0)
      val baseDec = Angle.fromDegrees(15.0)
      val base = Coordinates(RightAscension.fromAngle(baseRA), Declination.fromAngle(baseDec).getOrElse(Declination.zero))
      val matcher = criterion.matcher(base)

      // base pos is in radius limit with offset
      matcher.matches(target("testObj", base, List(new Magnitude(3.0, MagnitudeBand.J)))) should beTrue

      // Should be in mag and radius limit
      val pos = Coordinates(RightAscension.fromAngle(baseRA), Declination.fromAngle(baseDec + Angle.fromArcmin(11.0)).getOrElse(Declination.zero))
      matcher.matches(target("testObj", pos, List(new Magnitude(3.0, MagnitudeBand.J)))) should beTrue

      // Should be in mag and radius limit
      val pos2 = Coordinates(RightAscension.fromAngle(baseRA + Angle.fromArcmin(11.0)), Declination.fromAngle(baseDec).getOrElse(Declination.zero))
      matcher.matches(target("testObj", pos2, List(new Magnitude(3.0, MagnitudeBand.J)))) should beTrue
    }
    "support search with offset and pos angle" in {
      val radiusLimits = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val offset = Some(Offset(1.arcmins[OffsetP], 1.arcmins[OffsetQ]))
      val posAngle = Angle.fromDegrees(90.0).some
      val criterion = CatalogSearchCriterion("test", radiusLimits, mc, offset, posAngle)
      val baseRA = Angle.fromDegrees(10.0)
      val baseDec = Angle.fromDegrees(15.0)
      val base = Coordinates(RightAscension.fromAngle(baseRA), Declination.fromAngle(baseDec).getOrElse(Declination.zero))
      val matcher = criterion.matcher(base)

      // base pos is not in radius limit
      matcher.matches(target("testObj", base, List(new Magnitude(3.0, MagnitudeBand.J)))) should beFalse

      // Should not be in radius limit
      val pos = Coordinates(RightAscension.fromAngle(baseRA), Declination.fromAngle(baseDec + Angle.fromArcmin(11.0)).getOrElse(Declination.zero))
      matcher.matches(target("testObj", pos, List(new Magnitude(3.0, MagnitudeBand.J)))) should beFalse

      // Should be in mag and radius limit
      val pos2 = Coordinates(RightAscension.fromAngle(baseRA + Angle.fromArcmin(11.0)), Declination.fromAngle(baseDec).getOrElse(Declination.zero))
      matcher.matches(target("testObj", pos2, List(new Magnitude(3.0, MagnitudeBand.J)))) should beTrue

    }
  }
}

package edu.gemini.ags.gems

import edu.gemini.ags.impl._
import edu.gemini.catalog.api._
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw
import edu.gemini.spModel.gems.GemsGuideStarType
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

class GemsUtils4JavaSpec extends Specification {
  "GemsUtils4" should {
    val magnitudeConstraints = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(10.0), SaturationConstraint(2.0).some)
    "preserve the radius constraint for a single item without offsets" in {
      val key = new GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance)
      val radiusConstraint = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val criterion = CatalogSearchCriterion("test", magnitudeConstraints.some, radiusConstraint, None, None)

      val s = new GemsCatalogSearchCriterion(key, criterion)
      GemsUtils4Java.optimizeRadiusConstraint(List(s).asJava).maxLimit ~= radiusConstraint.maxLimit
      GemsUtils4Java.optimizeRadiusConstraint(List(s).asJava).minLimit ~= radiusConstraint.minLimit
    }
    "offset the radius constraint for a single item with offsets" in {
      val key = new GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance)
      val radiusConstraint = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val offset = Offset(Angle.fromArcmin(3), Angle.fromArcmin(4)).some
      val posAngle = Angle.fromArcmin(3).some
      val criterion = CatalogSearchCriterion("test", magnitudeConstraints.some, radiusConstraint, offset, posAngle)

      val s = new GemsCatalogSearchCriterion(key, criterion)
      GemsUtils4Java.optimizeRadiusConstraint(List(s).asJava).maxLimit ~= radiusConstraint.maxLimit + Angle.fromArcmin(5)
      GemsUtils4Java.optimizeRadiusConstraint(List(s).asJava).minLimit ~= radiusConstraint.minLimit
    }
    "find the max and min for a list of radius constraint without offsets" in {
      val key = new GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance)
      val radiusConstraint1 = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val radiusConstraint2 = RadiusConstraint.between(Angle.fromArcmin(15.0), Angle.fromArcmin(3.0))
      val criterion1 = CatalogSearchCriterion("test", magnitudeConstraints.some, radiusConstraint1, None, None)
      val criterion2 = CatalogSearchCriterion("test", magnitudeConstraints.some, radiusConstraint2, None, None)

      val s1 = new GemsCatalogSearchCriterion(key, criterion1)
      val s2 = new GemsCatalogSearchCriterion(key, criterion2)
      GemsUtils4Java.optimizeRadiusConstraint(List(s1, s2).asJava).maxLimit ~= Angle.fromArcmin(15.0)
      GemsUtils4Java.optimizeRadiusConstraint(List(s1, s2).asJava).minLimit ~= Angle.fromArcmin(2.0)
    }
    "find the max and min for a list of radius constraints with offsets" in {
      val key = new GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance)
      val radiusConstraint1 = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val radiusConstraint2 = RadiusConstraint.between(Angle.fromArcmin(15.0), Angle.fromArcmin(3.0))

      val offset1 = Offset(Angle.fromArcmin(3), Angle.fromArcmin(4)).some
      val offset2 = Offset(Angle.fromArcmin(5), Angle.fromArcmin(12)).some
      val posAngle = Angle.fromArcmin(3).some
      val criterion1 = CatalogSearchCriterion("test", magnitudeConstraints.some, radiusConstraint1, offset1, posAngle)
      val criterion2 = CatalogSearchCriterion("test", magnitudeConstraints.some, radiusConstraint2, offset2, posAngle)

      val s1 = new GemsCatalogSearchCriterion(key, criterion1)
      val s2 = new GemsCatalogSearchCriterion(key, criterion2)
      // Gets the offset from the largest offset distance (offset2 in this case)
      GemsUtils4Java.optimizeRadiusConstraint(List(s1, s2).asJava).maxLimit ~= (Angle.fromArcmin(15.0) + Angle.fromArcmin(13))
      GemsUtils4Java.optimizeRadiusConstraint(List(s1, s2).asJava).minLimit ~= Angle.fromArcmin(2.0)
    }
    "sort targets by R magnitude" in {
      import scala.collection.JavaConverters._
      val st1 = SiderealTarget("n", Coordinates.zero, None, List(new Magnitude(10.0, MagnitudeBand.J)), None)
      GemsUtils4Java.sortTargetsByBrightness(List(st1).asJava).get(0) should beEqualTo(st1)

      val st2 = SiderealTarget("n", Coordinates.zero, None, List(new Magnitude(15.0, MagnitudeBand.J)), None)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2).asJava).get(0) should beEqualTo(st1)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2).asJava).get(1) should beEqualTo(st2)

      val st3 = SiderealTarget("n", Coordinates.zero, None, List(new Magnitude(15.0, MagnitudeBand.R)), None)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3).asJava).get(0) should beEqualTo(st3)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3).asJava).get(1) should beEqualTo(st1)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3).asJava).get(2) should beEqualTo(st2)

      val st4 = SiderealTarget("n", Coordinates.zero, None, List(new Magnitude(9.0, MagnitudeBand.R)), None)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4).asJava).get(0) should beEqualTo(st4)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4).asJava).get(1) should beEqualTo(st3)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4).asJava).get(2) should beEqualTo(st1)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4).asJava).get(3) should beEqualTo(st2)

      val st5 = SiderealTarget("n", Coordinates.zero, None, List(new Magnitude(19.0, MagnitudeBand.R)), None)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4, st5).asJava).get(0) should beEqualTo(st4)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4, st5).asJava).get(1) should beEqualTo(st3)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4, st5).asJava).get(2) should beEqualTo(st5)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4, st5).asJava).get(3) should beEqualTo(st1)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4, st5).asJava).get(4) should beEqualTo(st2)
    }
  }
}

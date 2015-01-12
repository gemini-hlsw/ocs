package edu.gemini.ags.gems

import edu.gemini.catalog.api._
import edu.gemini.spModel.core.{AlmostEqual, Offset, MagnitudeBand, Angle}
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
      val criterion = CatalogSearchCriterion("test", magnitudeConstraints, radiusConstraint, None, None)

      val s = new GemsCatalogSearchCriterion(key, criterion)
      GemsUtils4Java.optimizeRadiusConstraint(List(s).asJava).maxLimit ~= radiusConstraint.maxLimit
      GemsUtils4Java.optimizeRadiusConstraint(List(s).asJava).minLimit ~= radiusConstraint.minLimit
    }
    "offset the radius constraint for a single item with offsets" in {
      val key = new GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance)
      val radiusConstraint = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val offset = Offset(Angle.fromArcmin(3), Angle.fromArcmin(4)).some
      val posAngle = Angle.fromArcmin(3).some
      val criterion = CatalogSearchCriterion("test", magnitudeConstraints, radiusConstraint, offset, posAngle)

      val s = new GemsCatalogSearchCriterion(key, criterion)
      GemsUtils4Java.optimizeRadiusConstraint(List(s).asJava).maxLimit ~= radiusConstraint.maxLimit + Angle.fromArcmin(5)
      GemsUtils4Java.optimizeRadiusConstraint(List(s).asJava).minLimit ~= radiusConstraint.minLimit
    }
    "find the max and min for a list of radius constraint without offsets" in {
      val key = new GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance)
      val radiusConstraint1 = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val radiusConstraint2 = RadiusConstraint.between(Angle.fromArcmin(15.0), Angle.fromArcmin(3.0))
      val criterion1 = CatalogSearchCriterion("test", magnitudeConstraints, radiusConstraint1, None, None)
      val criterion2 = CatalogSearchCriterion("test", magnitudeConstraints, radiusConstraint2, None, None)

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
      val criterion1 = CatalogSearchCriterion("test", magnitudeConstraints, radiusConstraint1, offset1, posAngle)
      val criterion2 = CatalogSearchCriterion("test", magnitudeConstraints, radiusConstraint2, offset2, posAngle)

      val s1 = new GemsCatalogSearchCriterion(key, criterion1)
      val s2 = new GemsCatalogSearchCriterion(key, criterion2)
      // Gets the offset from the largest offset distance (offset2 in this case)
      GemsUtils4Java.optimizeRadiusConstraint(List(s1, s2).asJava).maxLimit ~= (Angle.fromArcmin(15.0) + Angle.fromArcmin(13))
      GemsUtils4Java.optimizeRadiusConstraint(List(s1, s2).asJava).minLimit ~= Angle.fromArcmin(2.0)
    }
  }
}

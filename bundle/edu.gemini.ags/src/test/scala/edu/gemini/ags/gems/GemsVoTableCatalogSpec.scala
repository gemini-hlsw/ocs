package edu.gemini.ags.gems

import edu.gemini.catalog.api._
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.spModel.gemini.gems.CanopusWfs
import scala.concurrent.duration._
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.gemini.gems.GemsInstrument
import edu.gemini.spModel.gemini.gsaoi.{GsaoiOdgw, Gsaoi}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gems.{GemsGuideStarType, GemsTipTiltMode}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.IssPort
import AlmostEqual.AlmostEqualOps

import org.specs2.mutable.Specification

import scalaz._
import Scalaz._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

class GemsVoTableCatalogSpec extends Specification {
  val magnitudeRange = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(10.0), SaturationConstraint(2.0).some)

  "GemsVoTableCatalog" should {
    "support executing queries" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.SIDE_LOOKING)}
      val conditions = SPSiteQuality.Conditions.BEST
      val ctx = ObsContext.create(env, inst, JNone.instance[Site], conditions, null, null, JNone.instance())
      val base = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.instrument

      val posAngles = new java.util.HashSet[Angle]()
      val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)

      val results = Await.result(GemsVoTableCatalog(TestVoTableBackend("/gemsvotablecatalogquery.xml")).search(ctx, base, options, scala.None)(implicitly), 30.seconds)
      results should be size 2

      results.head.criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, GsaoiOdgw.Group.instance), CatalogSearchCriterion("On-detector Guide Window tiptilt", RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(14.5), Some(SaturationConstraint(7.3))), Some(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.None)))
      results(1).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.flexure, CanopusWfs.Group.instance), CatalogSearchCriterion("Canopus Wave Front Sensor flexure", RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), MagnitudeConstraints(RBandsList, FaintnessConstraint(16.8), Some(SaturationConstraint(9.3))), Some(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.None)))
      results.head.results should be size 5
      results(1).results should be size 5
    }
    "calculate the optimal radius limit" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val ctx = ObsContext.create(env, inst, JNone.instance[Site], SPSiteQuality.Conditions.BEST, null, null, JNone.instance())
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.instrument

      val posAngles = new java.util.HashSet[Angle]()
      val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)

      val results = GemsVoTableCatalog(TestVoTableBackend("/gemsvotablecatalogquery.xml")).getRadiusConstraints(instrument, options.searchCriteria(ctx, scala.None).asScala.toList)
      results should be size 1
      results.head should beEqualTo(RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01878572819686042)))
    }
    "calculate the optimal magnitude limit" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val ctx = ObsContext.create(env, inst, JNone.instance[Site], SPSiteQuality.Conditions.BEST, null, null, JNone.instance())
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.instrument

      val posAngles = new java.util.HashSet[Angle]()
      val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)

      val results = GemsVoTableCatalog(TestVoTableBackend("/gemsvotablecatalogquery.xml")).optimizeMagnitudeConstraints(options.searchCriteria(ctx, scala.None).asScala.toList)
      results should be size 2
      results.head should beEqualTo(MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(14.5), Some(SaturationConstraint(7.3))))
      results(1) should beEqualTo(MagnitudeConstraints(RBandsList, FaintnessConstraint(16.8), Some(SaturationConstraint(9.3))))
    }
    "preserve the radius constraint for a single item without offsets" in {
      val catalog = GemsVoTableCatalog(TestVoTableBackend(""))
      val key = GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance)
      val radiusConstraint = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val criterion = CatalogSearchCriterion("test", radiusConstraint, magnitudeRange, None, None)

      val s = new GemsCatalogSearchCriterion(key, criterion)
      (~catalog.optimizeRadiusConstraint(List(s)).map(_.maxLimit) ~= radiusConstraint.maxLimit) should beTrue
      (~catalog.optimizeRadiusConstraint(List(s)).map(_.minLimit) ~= radiusConstraint.minLimit) should beTrue
    }
    "offset the radius constraint for a single item with offsets" in {
      val catalog = GemsVoTableCatalog(TestVoTableBackend(""))
      val key = GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance)
      val radiusConstraint = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val offset = Offset(3.arcmins[OffsetP], 4.arcmins[OffsetQ]).some
      val posAngle = Angle.fromArcmin(3).some
      val criterion = CatalogSearchCriterion("test", radiusConstraint, magnitudeRange, offset, posAngle)

      val s = new GemsCatalogSearchCriterion(key, criterion)
      (~catalog.optimizeRadiusConstraint(List(s)).map(_.maxLimit) ~= radiusConstraint.maxLimit + Angle.fromArcmin(5)) should beTrue
      (~catalog.optimizeRadiusConstraint(List(s)).map(_.minLimit) ~= radiusConstraint.minLimit) should beTrue
    }
    "find the max and min for a list of radius constraint without offsets" in {
      val catalog = GemsVoTableCatalog(TestVoTableBackend(""))
      val key = GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance)
      val radiusConstraint1 = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val radiusConstraint2 = RadiusConstraint.between(Angle.fromArcmin(15.0), Angle.fromArcmin(3.0))
      val criterion1 = CatalogSearchCriterion("test", radiusConstraint1, magnitudeRange, None, None)
      val criterion2 = CatalogSearchCriterion("test", radiusConstraint2, magnitudeRange, None, None)

      val s1 = new GemsCatalogSearchCriterion(key, criterion1)
      val s2 = new GemsCatalogSearchCriterion(key, criterion2)
      (~catalog.optimizeRadiusConstraint(List(s1, s2)).map(_.maxLimit) ~= Angle.fromArcmin(15.0)) should beTrue
      (~catalog.optimizeRadiusConstraint(List(s1, s2)).map(_.minLimit) ~= Angle.fromArcmin(2.0)) should beTrue
    }
    "find the max and min for a list of radius constraints with offsets" in {
      val catalog = GemsVoTableCatalog(TestVoTableBackend(""))
      val key = GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance)
      val radiusConstraint1 = RadiusConstraint.between(Angle.fromArcmin(10.0), Angle.fromArcmin(2.0))
      val radiusConstraint2 = RadiusConstraint.between(Angle.fromArcmin(15.0), Angle.fromArcmin(3.0))

      val offset1 = Offset(3.arcmins[OffsetP], 4.arcmins[OffsetQ]).some
      val offset2 = Offset(5.arcmins[OffsetP], 12.arcmins[OffsetQ]).some
      val posAngle = Angle.fromArcmin(3).some
      val criterion1 = CatalogSearchCriterion("test", radiusConstraint1, magnitudeRange, offset1, posAngle)
      val criterion2 = CatalogSearchCriterion("test", radiusConstraint2, magnitudeRange, offset2, posAngle)

      val s1 = GemsCatalogSearchCriterion(key, criterion1)
      val s2 = GemsCatalogSearchCriterion(key, criterion2)
      // Gets the offset from the largest offset distance (offset2 in this case)
      (~catalog.optimizeRadiusConstraint(List(s1, s2)).map(_.maxLimit) ~= (Angle.fromArcmin(15.0) + Angle.fromArcmin(13))) should beTrue
      (~catalog.optimizeRadiusConstraint(List(s1, s2)).map(_.minLimit) ~= Angle.fromArcmin(2.0)) should beTrue
    }
  }
}

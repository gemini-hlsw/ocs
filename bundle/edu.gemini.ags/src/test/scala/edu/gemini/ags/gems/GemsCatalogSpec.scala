package edu.gemini.ags.gems

import edu.gemini.catalog.api.{SaturationConstraint, FaintnessConstraint, MagnitudeConstraints, RadiusConstraint}
import edu.gemini.spModel.gemini.gems.Canopus.Wfs
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._
import edu.gemini.shared.util.immutable.None
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.GemsInstrument
import edu.gemini.spModel.gemini.gsaoi.{GsaoiOdgw, Gsaoi}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gems.{GemsGuideStarType, GemsTipTiltMode}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.IssPort

import org.specs2.mutable.Specification

import scalaz._
import Scalaz._

import scala.concurrent.Await
import scala.collection.JavaConverters._

class GemsCatalogSpec extends Specification with NoTimeConversions {
  "GemsCatalog" should {
    "support executing queries" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val ctx = ObsContext.create(env, inst, None.instance[Site], SPSiteQuality.Conditions.BEST, null, null)
      val base = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
      val opticalCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG
      val nirCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.instrument

      val posAngles = new java.util.HashSet[Angle]()
      val options = new GemsGuideStarSearchOptions(opticalCatalog, nirCatalog,
              instrument, tipTiltMode, posAngles)

      try {
        val results = Await.result(GemsVoTableCatalog.search(ctx, base, options, scala.None, null), 30.seconds)
        results should be size 2

        results(0).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, GsaoiOdgw.Group.instance), CatalogSearchCriterion("On-detector Guide Window tiptilt", MagnitudeConstraints(MagnitudeBand.H, FaintnessConstraint(14.5), Some(SaturationConstraint(7.3))).some, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), Some(Offset(Angle.fromDegrees(0.0014984027777700248), Angle.fromDegrees(0.0014984027777700248))), scala.None)))
        results(1).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.flexure, Wfs.Group.instance), CatalogSearchCriterion("Canopus Wave Front Sensor flexure", MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(16.0), Some(SaturationConstraint(8.5))).some, RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), Some(Offset(Angle.fromDegrees(0.0014984027777700248), Angle.fromDegrees(0.0014984027777700248))), scala.None)))
        results(0).results should be size 5
        results(1).results should be size 3
      } catch {
        case e:Exception =>
          skipped("Catalog may be down")
      }
    }
    "calculate the optimal radius limit" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val ctx = ObsContext.create(env, inst, None.instance[Site], SPSiteQuality.Conditions.BEST, null, null)
      val opticalCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG
      val nirCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.instrument

      val posAngles = new java.util.HashSet[Angle]()
      val options = new GemsGuideStarSearchOptions(opticalCatalog, nirCatalog,
              instrument, tipTiltMode, posAngles)

      val results = GemsVoTableCatalog.getRadiusLimits(instrument, options.searchCriteria(ctx, scala.None).asScala.toList)
      results should be size 1
      results(0) should beEqualTo(RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01878572819686042)))
    }
    "calculate the optimal magnitude limit" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val ctx = ObsContext.create(env, inst, None.instance[Site], SPSiteQuality.Conditions.BEST, null, null)
      val opticalCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG
      val nirCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.instrument

      val posAngles = new java.util.HashSet[Angle]()
      val options = new GemsGuideStarSearchOptions(opticalCatalog, nirCatalog,
              instrument, tipTiltMode, posAngles)

      val results = GemsVoTableCatalog.optimizeMagnitudeLimits(options.searchCriteria(ctx, scala.None).asScala.toList)
      results should be size 2
      results(0) should beEqualTo(MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(16), Some(SaturationConstraint(8.5))))
      results(1) should beEqualTo(MagnitudeConstraints(MagnitudeBand.H, FaintnessConstraint(14.5), Some(SaturationConstraint(7.3))))
    }
  }
}
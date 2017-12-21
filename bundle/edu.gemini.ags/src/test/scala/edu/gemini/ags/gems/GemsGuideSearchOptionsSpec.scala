package edu.gemini.ags.gems

import edu.gemini.catalog.api._
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.{CanopusWfs, GemsInstrument}
import edu.gemini.spModel.gemini.gsaoi.{GsaoiOdgw, Gsaoi}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gems.{GemsGuideStarType, GemsTipTiltMode}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.IssPort
import org.specs2.mutable.Specification
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

class GemsGuideSearchOptionsSpec extends Specification {
  val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
  val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
  val target = new SPTarget(ra.toDegrees, dec.toDegrees)
  val targetEnvironment = TargetEnvironment.create(target)
  val inst = new Gsaoi
  inst.setPosAngle(0.0)
  inst.setIssPort(IssPort.SIDE_LOOKING)
  val ctx = ObsContext.create(targetEnvironment, inst, JNone.instance[Site], SPSiteQuality.Conditions.BEST, null, null, JNone.instance())
  val posAngles = new java.util.HashSet[Angle]()

  "GemsGuideSearchOptions" should {
    "provide search options for gsaoi in instrument tip tilt mode" in {
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.instrument

      val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)
      val criteria = options.searchCriteria(ctx, None).asScala

      criteria should be size 2
      criteria.head.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, GsaoiOdgw.Group.instance))
      criteria.head.criterion.magConstraint should beEqualTo(MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(14.5), Some(SaturationConstraint(7.3))))
      criteria(1).key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, CanopusWfs.Group.instance))
      criteria(1).criterion.magConstraint should beEqualTo(MagnitudeConstraints(RBandsList, FaintnessConstraint(18.0), Some(SaturationConstraint(11.5))))
    }
    "provide search options for gsaoi in canopus tip tilt mode" in {
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.canopus

      val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)
      val criteria = options.searchCriteria(ctx, None).asScala

      criteria should be size 2
      criteria.head.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, CanopusWfs.Group.instance))
      criteria.head.criterion.magConstraint should beEqualTo(MagnitudeConstraints(RBandsList, FaintnessConstraint(18.0), Some(SaturationConstraint(11.5))))
      criteria(1).key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance))
      criteria(1).criterion.magConstraint should beEqualTo(MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(17.0), Some(SaturationConstraint(8.0))))
    }
    "provide search options for gsaoi in both tip tilt modes" in {
      val ctx = ObsContext.create(targetEnvironment, inst, JNone.instance[Site], SPSiteQuality.Conditions.BEST, null, null, JNone.instance())
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.both

      val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)
      val criteria = options.searchCriteria(ctx, None).asScala

      criteria should be size 4
      criteria.head.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, CanopusWfs.Group.instance))
      criteria.head.criterion.magConstraint should beEqualTo(MagnitudeConstraints(RBandsList, FaintnessConstraint(18.0), Some(SaturationConstraint(11.5))))
      criteria(1).key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance))
      criteria(1).criterion.magConstraint should beEqualTo(MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(17.0), Some(SaturationConstraint(8.0))))
      criteria(2).key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, GsaoiOdgw.Group.instance))
      criteria(2).criterion.magConstraint should beEqualTo(MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(14.5), Some(SaturationConstraint(7.3))))
      criteria(3).key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, CanopusWfs.Group.instance))
      criteria(3).criterion.magConstraint should beEqualTo(MagnitudeConstraints(RBandsList, FaintnessConstraint(18.0), Some(SaturationConstraint(11.5))))
    }
  }
}

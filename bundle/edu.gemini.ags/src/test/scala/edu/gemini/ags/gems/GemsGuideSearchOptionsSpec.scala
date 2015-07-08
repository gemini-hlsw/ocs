package edu.gemini.ags.gems

import edu.gemini.catalog.api.{MagnitudeRange, SaturationConstraint, FaintnessConstraint}
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.spModel.core.{MagnitudeBand, Site, Angle}
import edu.gemini.spModel.gemini.gems.{Canopus, GemsInstrument}
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
  val env = TargetEnvironment.create(target)
  val inst = new Gsaoi
  inst.setPosAngle(0.0)
  inst.setIssPort(IssPort.SIDE_LOOKING)
  val ctx = ObsContext.create(env, inst, JNone.instance[Site], SPSiteQuality.Conditions.BEST, null, null)
  val posAngles = new java.util.HashSet[Angle]()

  "GemsGuideSearchOptions" should {
    "provide search options for gsaoi in instrument tip tilt mode" in {
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.instrument

      val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)
      val criteria = options.searchCriteria(ctx, None).asScala

      criteria should be size 2
      criteria.head.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, GsaoiOdgw.Group.instance))
      criteria.head.criterion.referenceBands should beEqualTo(List(MagnitudeBand.H))
      criteria.head.criterion.magRange should beEqualTo(MagnitudeRange(FaintnessConstraint(14.5), Some(SaturationConstraint(7.3))))
      criteria(1).key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, Canopus.Wfs.Group.instance))
      criteria(1).criterion.referenceBands should beEqualTo(List(MagnitudeBand.R))
      criteria(1).criterion.magRange should beEqualTo(MagnitudeRange(FaintnessConstraint(16.0), Some(SaturationConstraint(8.5))))
    }
    "provide search options for gsaoi in canopus tip tilt mode" in {
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.canopus

      val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)
      val criteria = options.searchCriteria(ctx, None).asScala

      criteria should be size 2
      criteria.head.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, Canopus.Wfs.Group.instance))
      criteria.head.criterion.referenceBands should beEqualTo(List(MagnitudeBand.R))
      criteria.head.criterion.magRange should beEqualTo(MagnitudeRange(FaintnessConstraint(16.0), Some(SaturationConstraint(8.5))))
      criteria(1).key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance))
      criteria(1).criterion.referenceBands should beEqualTo(List(MagnitudeBand.H))
      criteria(1).criterion.magRange should beEqualTo(MagnitudeRange(FaintnessConstraint(17.0), Some(SaturationConstraint(8))))
    }
    "provide search options for gsaoi in both tip tilt modes" in {
      val ctx = ObsContext.create(env, inst, JNone.instance[Site], SPSiteQuality.Conditions.BEST, null, null)
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.both

      val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)
      val criteria = options.searchCriteria(ctx, None).asScala

      criteria should be size 4
      criteria.head.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, Canopus.Wfs.Group.instance))
      criteria.head.criterion.referenceBands should beEqualTo(List(MagnitudeBand.R))
      criteria.head.criterion.magRange should beEqualTo(MagnitudeRange(FaintnessConstraint(16.0), Some(SaturationConstraint(8.5))))
      criteria(1).key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance))
      criteria(1).criterion.referenceBands should beEqualTo(List(MagnitudeBand.H))
      criteria(1).criterion.magRange should beEqualTo(MagnitudeRange(FaintnessConstraint(17.0), Some(SaturationConstraint(8))))
      criteria(2).key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, GsaoiOdgw.Group.instance))
      criteria(2).criterion.referenceBands should beEqualTo(List(MagnitudeBand.H))
      criteria(2).criterion.magRange should beEqualTo(MagnitudeRange(FaintnessConstraint(14.5), Some(SaturationConstraint(7.3))))
      criteria(3).key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, Canopus.Wfs.Group.instance))
      criteria(3).criterion.referenceBands should beEqualTo(List(MagnitudeBand.R))
      criteria(3).criterion.magRange should beEqualTo(MagnitudeRange(FaintnessConstraint(16.0), Some(SaturationConstraint(8.5))))
    }
  }
}

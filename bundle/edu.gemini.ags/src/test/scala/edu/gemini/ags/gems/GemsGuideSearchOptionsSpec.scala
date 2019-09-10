package edu.gemini.ags.gems

import edu.gemini.catalog.api._
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.{CanopusWfs, GemsInstrument}
import edu.gemini.spModel.gemini.gsaoi.{GsaoiOdgw, Gsaoi}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gems.GemsGuideStarType
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.IssPort
import org.specs2.mutable.Specification
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

class GemsGuideSearchOptionsSpec extends Specification {
  val ra     = Angle.fromHMS( 3, 19, 48.2341).getOrElse(Angle.zero)
  val dec    = Angle.fromDMS(41, 30, 42.0780).getOrElse(Angle.zero)
  val target = new SPTarget(ra.toDegrees, dec.toDegrees)
  val targetEnvironment = TargetEnvironment.create(target)
  val inst = new Gsaoi
  inst.setPosAngle(0.0)
  inst.setIssPort(IssPort.SIDE_LOOKING)
  val ctx = ObsContext.create(targetEnvironment, inst, JNone.instance[Site], SPSiteQuality.Conditions.BEST, null, null, JNone.instance())
  val posAngles = new java.util.HashSet[Angle]()

  "GemsGuideSearchOptions" should {
    "provide search options for canopus" in {
      val ctx        = ObsContext.create(targetEnvironment, inst, JNone.instance[Site], SPSiteQuality.Conditions.BEST, null, null, JNone.instance())
      val instrument = GemsInstrument.gsaoi

      val options  = new GemsGuideStarSearchOptions(instrument, posAngles)
      val criteria = options.canopusCriterion(ctx)

      criteria.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, CanopusWfs.Group.instance))
      criteria.criterion.magConstraint should beEqualTo(MagnitudeConstraints(RBandsList, FaintnessConstraint(17.0), Some(SaturationConstraint(10.5))))

    }
  }
}

package edu.gemini.ags.gems

import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.api._
import edu.gemini.catalog.api.CatalogName.PPMXL
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.spModel.gemini.gems.CanopusWfs
import scala.concurrent.duration._
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.gemini.gsaoi.{GsaoiOdgw, Gsaoi}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gems.GemsGuideStarType
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

      val mod = GemsTestVoTableMod.forCwfsMagnitudeLimitChange(conditions)
      val results = Await.result(GemsVoTableCatalog(PPMXL, Some(TestVoTableBackend("/gemsvotablecatalogquery.xml", mod))).search(ctx, ProbeLimitsTable.loadOrThrow)(implicitly), 30.seconds)

      results should be size 5
    }
  }
}

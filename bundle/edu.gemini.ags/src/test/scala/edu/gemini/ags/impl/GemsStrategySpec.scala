package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsStrategy.Estimate
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.shared.util.immutable.Some
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.spModel.gemini.gsaoi.{Gsaoi, GsaoiOdgw}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import edu.gemini.spModel.telescope.IssPort
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

class GemsStrategySpec extends Specification with NoTimeConversions {
  "GemsStrategy" should {
    "support estimate" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val gsaoi = GsaoiOdgw.values().toList
      val canopus = Canopus.Wfs.values().toList
      val pwfs1 = List(PwfsGuideProbe.pwfs1)
      val guiders:List[GuideProbe] = gsaoi ::: canopus ::: pwfs1

      val ctx = ObsContext.create(env.setActiveGuiders(guiders.toSet.asJava), inst, new Some(Site.GS), SPSiteQuality.Conditions.BEST, null, null)

      try {
        val estimate = GemsStrategy.estimate(ctx, ProbeLimitsTable.loadOrThrow())
        Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
      } catch {
        case e:Exception =>
          skipped("Catalog may be down")
      }
    }
  }
}

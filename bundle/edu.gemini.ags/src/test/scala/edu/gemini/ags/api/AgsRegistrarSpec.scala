package edu.gemini.ags.api

import edu.gemini.ags.impl.SingleProbeStrategy
import edu.gemini.ags.impl.SingleProbeStrategyParams.PwfsParams
import edu.gemini.catalog.votable.ConeSearchBackend
import edu.gemini.shared.util.immutable.{None => JNone, Some => JSome}
import edu.gemini.spModel.ags.AgsStrategyKey.Pwfs2SouthKey
import edu.gemini.spModel.core.{Site, Declination, Angle}
import edu.gemini.spModel.gemini.phoenix.InstPhoenix
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._

class AgsRegistrarSpec extends Specification {
  "AgsRegistrar" should {
    "provide a strategy for phoenix using PWFS2" in {
      val ra = Angle.fromHMS(5, 47, 22.207).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(51, 2, 14.650).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new InstPhoenix <| {_.setPosAngle(0.0)}

      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), null, null, null, JNone.instance())

      AgsRegistrar.defaultStrategy(ctx) should beSome(SingleProbeStrategy(Pwfs2SouthKey,PwfsParams(Site.GS, PwfsGuideProbe.pwfs2)))
    }
  }

}

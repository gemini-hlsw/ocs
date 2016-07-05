package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsStrategy
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe.{instance => GmosOiwfs}
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.{AutomaticGroup, TargetEnv, TargetEnvironment}
import org.junit.Test
import org.junit.Assert

import scalaz.==>>

/** Test case for REL-2874. */
class PosAngleUpdateTest extends Helpers {

  @Test def testPosAngleUpdate(): Unit = {
    // Set up a TargetEnvironment according to the description in REL-2874.
    val base    = basePosition("02:22:32.907 42:20:53.95")
    val gs      = siderealTarget("663-010421", "02:22:20.970 42:24:44.27", 13.805)
    val tmap    = ==>>.singleton(GmosOiwfs: GuideProbe, new SPTarget(gs))
    val ten     = Angle.fromDegrees(10.0)
    val envTen  = TargetEnv.auto.set(TargetEnvironment.create(base), AutomaticGroup.Active(tmap, ten))

    // Create a new selection with the same guide star mapping, but a new pos angle.
    val ten5    = Angle.fromDegrees(10.5)
    val sel     = AgsStrategy.Selection(ten5, List(AgsStrategy.Assignment(GmosOiwfs, gs)))

    // Apply the selection to the existing target environment.
    val envTen5 = sel.applyTo(envTen)

    // Get the resulting position angle in the new environment.
    val actual  = TargetEnv.auto.get(envTen5) match {
      case AutomaticGroup.Active(_, pa) => Some(pa)
      case _                            => None
    }

    // Make sure it is now 10.5
    Assert.assertEquals(Some(ten5), actual)
  }
}

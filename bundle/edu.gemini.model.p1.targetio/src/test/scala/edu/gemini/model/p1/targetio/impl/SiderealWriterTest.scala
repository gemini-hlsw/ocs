package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.targetio.impl.TargetUtil._

import edu.gemini.model.p1.immutable.SiderealTarget
import edu.gemini.spModel.core.{MagnitudeBand, MagnitudeSystem}

class SiderealWriterTest extends WriterTestBase(SiderealReader, SiderealWriter) {
  val target1 = mkTarget("ngc001", "01:00:00.00", "02:00:00", 1.0, 2.0)
  val target2 = mkTarget("ngc002", "10:00:00.00", "20:00:00", 0.1, 0.2)
  val target3 = mkTarget("ngc003", "11:00:00.00", "22:00:00", 2.2, 2.2)
  val target4 = mkTarget("ngc004", "00:00:00.00", "00:00:00", 0.0, 0.0)
  val target5 = mkTarget("ngc004", "00:00:00.00", "00:00:00")

  val targets = List(
    target1.copy(magnitudes = List(mkMag(7.0, MagnitudeBand.J, MagnitudeSystem.Jy))),
    target2.copy(magnitudes = List(mkMag(8.0, MagnitudeBand.H))),
    target3.copy(magnitudes = List(mkMag(1.0, MagnitudeBand.J), mkMag(2.0, MagnitudeBand.H), mkMag(3.0, MagnitudeBand.K, MagnitudeSystem.Jy))),
    target4,
    target5
  )

  def mkTargets = targets

  def validateTarget(expected: SiderealTarget, actual: SiderealTarget) {
    TargetUtil.validateTarget(expected, actual)
  }

}
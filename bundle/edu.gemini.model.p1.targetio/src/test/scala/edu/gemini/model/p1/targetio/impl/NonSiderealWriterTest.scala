package edu.gemini.model.p1.targetio.impl

import TargetUtil._

import edu.gemini.model.p1.targetio.api.FileType.Csv

import org.junit.Test
import edu.gemini.model.p1.immutable.NonSiderealTarget

class NonSiderealWriterTest extends WriterTestBase(NonSiderealReader, NonSiderealWriter) {
  val targets = List(
    mkTarget("Ceres",  List(dec30, dec31, jan01)),
    mkTarget("Halley", List(aug15, aug16, aug17))
  )

  def mkTargets = targets

  @Test def testOptionalMagColumn() {
    val t = mkTarget("Ceres",  List(dec30.copy(magnitude = None), dec31.copy(magnitude = None)))
    io(List(t), Csv)
  }

  def validateTarget(expected: NonSiderealTarget, actual: NonSiderealTarget) {
    TargetUtil.validateTarget(expected, actual)
  }

}
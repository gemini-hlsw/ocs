package edu.gemini.phase2.skeleton.factory

import org.junit.Test
import org.junit.Assert._
import edu.gemini.model.p1.{mutable => M}
import edu.gemini.model.p1.immutable.{GracesBlueprint, GsaoiBlueprint}
import edu.gemini.spModel.`type`.SpTypeUtil
import edu.gemini.pot.sp.SPComponentType

class SpBlueprintFactoryTest {
  @Test
  def buildGsoai() {
    val gsoaiBlueprint = GsaoiBlueprint(M.GsaoiFilter.J :: Nil)
    val factory = SpBlueprintFactory.create(gsoaiBlueprint)
    assertTrue(factory.isRight)
  }

  @Test
  def allGsoaiFiltersPresentInP2() {
    val enumNotInOT = M.GsaoiFilter.values() map {
      f => Option(SpTypeUtil.noExceptionValueOf(classOf[M.GsaoiFilter], f.toString))
    } filter {
      _.isEmpty
    }

    assertTrue(enumNotInOT.isEmpty)
  }

  @Test
  def allGpiObservingModesPresentInP2() {
    val enumNotInOT = M.GpiObservingMode.values() map {
      f => Option(SpTypeUtil.noExceptionValueOf(classOf[M.GpiObservingMode], f.toString))
    } filter {
      _.isEmpty
    }

    assertTrue(enumNotInOT.isEmpty)
  }

  @Test
  def allF2FiltersPresentInP2(): Unit = {
    val enumNotInOT = M.Flamingos2Filter.values() map {
      f => Option(SpTypeUtil.noExceptionValueOf(classOf[M.Flamingos2Filter], f.toString))
    } filter {
      _.isEmpty
    }

    assertTrue(enumNotInOT.isEmpty)
  }

  @Test
  def buildGraces() {
    val gracesBlueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER, M.GracesReadMode.NORMAL)
    val factory = SpBlueprintFactory.create(gracesBlueprint)
    assertTrue(factory.isRight)
    assertEquals(SPComponentType.INSTRUMENT_VISITOR, factory.right.get.instrumentType())
  }

}

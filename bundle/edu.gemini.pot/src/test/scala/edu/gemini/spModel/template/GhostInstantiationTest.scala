package edu.gemini.spModel.template

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.gemini.ghost.Ghost
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.gemini.ghost.blueprint.SpGhostBlueprint
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.target.env.AsterismType
import edu.gemini.spModel.obsclass.ObsClass.SCIENCE
import edu.gemini.spModel.target.env.Asterism
import edu.gemini.spModel.util.SPTreeUtil
import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConverters._


class GhostInstantiationTest extends TestBase {

  def instantiate(astType: AsterismType, os: ISPObservation*): List[ISPObservation] = {

    val fact = getFactory
    val prog = getProgram

    // Remove existing observations
    prog.setObservations(java.util.Collections.emptyList())
    prog.setGroups(java.util.Collections.emptyList())

    // Add the template folder with a single GHOST blueprint
    val tfNode = fact.createTemplateFolder(prog, WithSomeNewKey)
    val bp: SpBlueprint = SpGhostBlueprint.fromAsterismType(astType).getValue
    val bpMap = Map("blueprint-0" -> bp)
    val tf    = new TemplateFolder(bpMap.asJava)
    tfNode.setDataObject(tf)

    val tgNode = fact.createTemplateGroup(prog, WithSomeNewKey)
    val tg     = new TemplateGroup()
    tg.setBlueprintId("blueprint-0")
    tgNode.setDataObject(tg)
    os.foreach { o =>
      setAsterismType(o, astType)
      tgNode.addObservation(o)
    }

    val tpNode = fact.createTemplateParameters(prog, WithSomeNewKey)
    val tp     = newParameters(ScienceTargetName, ScienceTargetCC)
    tpNode.setDataObject(tp)

    tgNode.addTemplateParameters(tpNode)
    tfNode.addTemplateGroup(tgNode)
    prog.setTemplateFolder(tfNode)

    // Instantiate observations
    val func = new InstantiationFunctor
    func.add(tgNode, tpNode)
    func.execute(getOdb, getProgram, null)

    val grp = getProgram.getGroups.get(0)
    grp.getObservations.asScala.toList
  }


  private def checkDiff(a: Coordinates, b: Coordinates, raArcmin: Double, decArcmin: Double): Unit = {
    val (r, d) = a.diff(b)

    val delta = 0.0001
    assertEquals(raArcmin,  r.toSignedDegrees * 60, delta)
    assertEquals(decArcmin, d.toSignedDegrees * 60, delta)
  }

  private def checkDualTarget(a: Asterism): Unit = {
    val ct = newTarget(ScienceTargetName).getCoordinates(None).get

    assertEquals(AsterismType.GhostDualTarget, a.asterismType)
    a match {
      case GhostAsterism.DualTarget(ifu1, ifu2, base) =>
        // Overridden base at the template target coordinates
         checkDiff(base.get.coordinates, ct, 0, 0)

        // IFU1 at 1 arcmin (in p) from base
        checkDiff(ifu1.coordinates(None).get, ct, 1, 0)

        // IFU2 at -1 arcmin (in p) from base
        checkDiff(ifu2.coordinates(None).get, ct, -1, 0)

      case _ =>
        fail(s"Expected a Ghost Dual Target asterism, not $a")
    }

  }

  private def setAsterismType(o: ISPObservation, asterismType: AsterismType): Unit = {
    val shell = SPTreeUtil.findInstrument(o)
    val ghost = shell.getDataObject.asInstanceOf[Ghost]
    ghost.setPreferredAsterismType(asterismType)
    shell.setDataObject(ghost)
  }

  @Test def testInstantiationHasProperAsterism(): Unit = {
    val obsList = instantiate(AsterismType.GhostDualTarget, newObs(SCIENCE, Instrument.Ghost))
    obsList match {
      case o :: Nil => checkDualTarget(target(o).map(_.getAsterism).get)
      case _        => fail("Expected a single observation")
    }
  }
}
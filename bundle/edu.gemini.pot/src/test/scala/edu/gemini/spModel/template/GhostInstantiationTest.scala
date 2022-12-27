package edu.gemini.spModel.template

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.gemini.ghost.blueprint.SpGhostBlueprint
import edu.gemini.spModel.target.env.AsterismType
import edu.gemini.spModel.obsclass.ObsClass.SCIENCE
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
    tg.setAsterismType(astType)
    tgNode.setDataObject(tg)
    os.foreach(tgNode.addObservation)

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

  @Test def testInstantiationHasProperAsterism(): Unit = {
    val obsList = instantiate(AsterismType.GhostDualTarget, newObs(SCIENCE, Instrument.Ghost))

    obsList match {
      case o :: Nil =>
        target(o).map(_.getAsterism.asterismType).contains(AsterismType.GhostDualTarget)

      case _        =>
        fail("Expected a single observation")

    }
  }
}
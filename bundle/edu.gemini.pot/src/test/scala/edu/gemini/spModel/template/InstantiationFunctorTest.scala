package edu.gemini.spModel.template

import edu.gemini.pot.sp.{Instrument, ISPObservation}
import edu.gemini.pot.sp.SPComponentType.INSTRUMENT_GMOSSOUTH
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth
import edu.gemini.spModel.gemini.gmos.InstGmosSouth
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintImaging
import edu.gemini.spModel.gemini.security.UserRolePrivileges
import edu.gemini.spModel.obsclass.ObsClass._

import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._


class InstantiationFunctorTest extends TestBase {

  def instantiate(os: ISPObservation*): List[ISPObservation] = {
    val fact = getFactory
    val prog = getProgram

    // Remove any existing observations.
    prog.setObservations(java.util.Collections.emptyList())
    prog.setGroups(java.util.Collections.emptyList())

    // Setup the test program with a single template group with a single target
    // at (RA, Dec) (1, 2) and CC50 observing conditions.
    val tfNode = fact.createTemplateFolder(prog, WithSomeNewKey)

    val bp: SpBlueprint = new SpGmosSBlueprintImaging(List(FilterSouth.g_G0325).asJava)
    val bpMap = Map("blueprint-0" -> bp)
    val tf    = new TemplateFolder(bpMap.asJava)
    tfNode.setDataObject(tf)

    val tgNode = fact.createTemplateGroup(prog, WithSomeNewKey)
    val tg     = new TemplateGroup()
    tg.setBlueprintId("blueprint-0")
    tgNode.setDataObject(tg)

    val tpNode = fact.createTemplateParameters(prog, WithSomeNewKey)
    val tp     = newParameters(ScienceTargetName, ScienceTargetCC)
    tpNode.setDataObject(tp)

    tgNode.addTemplateParameters(tpNode)
    tfNode.addTemplateGroup(tgNode)
    prog.setTemplateFolder(tfNode)

    // Add an observation for each of the specified obs classes
    os.foreach(tgNode.addObservation)

    // Instantiate observations
    val func = new InstantiationFunctor
    func.add(tgNode, tpNode)
    func.execute(getOdb, getProgram, null)

    // Return the instantiated observations
    val grp = getProgram.getGroups.get(0)
    grp.getObservations.asScala.toList
  }

  def reapply(os: ISPObservation*): List[ISPObservation] = {
    val func = new ReapplicationFunctor(UserRolePrivileges.STAFF)
    os.foreach(func.add)
    func.execute(getOdb, null, null)

    // If the functor ended with an exception, rethrow it so it stops the test
    // as well.
    Option(func.getException).foreach { ex => throw ex }

    val obsKeys = os.map(_.getNodeKey).toSet
    getProgram.getAllObservations.asScala.toList.filter(o => obsKeys(o.getNodeKey))
  }


  def getPositionAngle(o: ISPObservation): Double =
    obsComp[InstGmosSouth](o, INSTRUMENT_GMOSSOUTH).map {
      case (_, gmos) => gmos.getPosAngleDegrees
    } | sys.error("expected gmos component")

  def setPositionAngle(o: ISPObservation, pa: Double): Unit =
    obsComp[InstGmosSouth](o, INSTRUMENT_GMOSSOUTH).foreach {
      case (oc, gmos) => gmos.setPosAngleDegrees(pa)
                         oc.setDataObject(gmos)
      case _          => fail("expected gmos component")
    }

  override def editObservation(o: ISPObservation): Unit = {
    super.editObservation(o)
    setPositionAngle(o, EditPA)
  }

  @Test def testDayCalGetsNeitherConditionsNorTarget(): Unit = {
    val obsList = instantiate(newObs(DAY_CAL, Instrument.GmosSouth))

    obsList match {
      case o :: Nil =>
        // Neither science site quality nor science target are copied.
        assertTrue(siteQuality(o).isEmpty && target(o).isEmpty)
      case _        =>
        fail("Expected a single observation")
    }
  }

  @Test def testReapplyDayCal(): Unit = {
    val obsList = instantiate(newObs(DAY_CAL, Instrument.GmosSouth))
    obsList.foreach(editObservation)

    reapply(obsList: _*) match {
      case o :: Nil =>
        // Everything has been reset.
        assertTrue(siteQuality(o).isEmpty && target(o).isEmpty && getPositionAngle(o) == 0.0)
      case _        =>
        fail("Expected a single observation")
    }

  }

  @Test def testNightCalHasScienceConditions(): Unit = {
    List(ACQ_CAL, PARTNER_CAL, PROG_CAL).foreach { c =>
      val obsList = instantiate(newObs(c, Instrument.GmosSouth))

      obsList match {
        case o :: Nil =>
          // Science site quality is copied, but not the science target
          assertTrue(siteQuality(o).exists(_.getCloudCover == ScienceTargetCC) &&
                     target(o).isEmpty)
        case _       =>
          fail("Expected a single observation")
      }
    }
  }

  @Test def testScienceHasScienceConditionsAndTarget(): Unit = {
    List(ACQ, SCIENCE).foreach { c =>
      val obsList = instantiate(newObs(c, Instrument.GmosSouth))

      obsList match {
        case o :: Nil =>
          // Science site quality is copied, but not the science target
          assertTrue(siteQuality(o).exists(_.getCloudCover == ScienceTargetCC) &&
                     target(o).exists(_.getAsterism.name == ScienceTargetName))
        case _       =>
          fail("Expected a single observation")
      }
    }
  }

  @Test def testReapplyScienceAndCal(): Unit = {
    List(ACQ, SCIENCE, ACQ_CAL, PARTNER_CAL, PROG_CAL).foreach { c =>
      val obsList = instantiate(newObs(c, Instrument.GmosSouth))
      obsList.foreach(editObservation)

      reapply(obsList: _*) match {
        case o :: Nil =>
          // CC, target, and PA changes are kept.
          assertTrue(siteQuality(o).exists(_.getCloudCover == EditTargetCC) &&
                     target(o).exists(_.getAsterism.name == EditTargetName) &&
                     getPositionAngle(o) == EditPA)
        case _       =>
          fail("Expected a single observation")
      }
    }
  }
}
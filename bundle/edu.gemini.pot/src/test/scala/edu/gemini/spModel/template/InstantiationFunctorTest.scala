package edu.gemini.spModel.template

import edu.gemini.pot.sp.{Instrument, ISPObservation}
import edu.gemini.pot.sp.SPComponentType.INSTRUMENT_GMOSSOUTH
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth
import edu.gemini.spModel.gemini.gmos.InstGmosSouth
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintImaging
import edu.gemini.spModel.gemini.security.UserRolePrivileges
import edu.gemini.spModel.obsclass.ObsClass._
import edu.gemini.spModel.rich.pot.sp._

import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._


class InstantiationFunctorTest extends TestBase {

  def instantiate(os: ISPObservation*): List[ISPObservation] =
    instantiate2(List(("blueprint-0", os.toList, List(newParameters(ScienceTargetName, ScienceTargetCC)))))

  def instantiate2(
    groups: List[(String, List[ISPObservation], List[TemplateParameters])]
  ): List[ISPObservation] = {
    val fact = getFactory
    val prog = getProgram

    // Remove any existing observations.
    prog.setObservations(java.util.Collections.emptyList())
    prog.setGroups(java.util.Collections.emptyList())

    // Setup the test program with a single template group with a single target
    // at (RA, Dec) (1, 2) and CC50 observing conditions.
    val tfNode = fact.createTemplateFolder(prog, WithSomeNewKey)

    val bpMap = groups.map { case (blueprintId, _, _) =>
      val bp: SpBlueprint = new SpGmosSBlueprintImaging(List(FilterSouth.g_G0325).asJava)
      (blueprintId, bp)
    }.toMap

    val tf = new TemplateFolder(bpMap.asJava)
    tfNode.setDataObject(tf)

    val func = new InstantiationFunctor

    groups.foreach { case (blueprintId, os, params) =>

      val tgNode = fact.createTemplateGroup(prog, WithSomeNewKey)
      val tg     = new TemplateGroup()
      tg.setBlueprintId(blueprintId)
      tgNode.setDataObject(tg)

      val tpNodes = params.map { tp =>
        val tpNode = fact.createTemplateParameters(prog, WithSomeNewKey)
        tpNode.setDataObject(tp)
        tpNode
      }
      tpNodes.foreach(tgNode.addTemplateParameters)

      // Add an observation for each of the specified obs classes
      os.foreach(tgNode.addObservation)

      tfNode.addTemplateGroup(tgNode)

      tpNodes.foreach(func.add(tgNode, _))
    }

    prog.setTemplateFolder(tfNode)

    func.execute(getOdb, getProgram, null)

    // Return the instantiated observations
    getProgram.getGroups.asScala.toList.flatMap { grp =>
      grp.getObservations.asScala.toList
    }
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

  @Test def testInstantiationOrderOneGroup(): Unit = {
    val obsList = instantiate2(
      List((
        "blueprint-0",
        List(newObs(SCIENCE, Instrument.GmosSouth)),
        List(
          newParameters(s"$ScienceTargetName-01", ScienceTargetCC),
          newParameters(s"$ScienceTargetName-02", ScienceTargetCC),
          newParameters(s"$ScienceTargetName-03", ScienceTargetCC),
          newParameters(s"$ScienceTargetName-04", ScienceTargetCC),
          newParameters(s"$ScienceTargetName-05", ScienceTargetCC),
          newParameters(s"$ScienceTargetName-06", ScienceTargetCC),
          newParameters(s"$ScienceTargetName-07", ScienceTargetCC),
          newParameters(s"$ScienceTargetName-08", ScienceTargetCC),
          newParameters(s"$ScienceTargetName-09", ScienceTargetCC),
          newParameters(s"$ScienceTargetName-10", ScienceTargetCC)
        )
      ))
    )

    val names = obsList.map { obs =>
      obs.findTargetObsComp.get.getTargetEnvironment.getTargets.get(0).getName
    }
    assertEquals(10, obsList.length);
    assertEquals(names.sorted, names)
  }

  @Test def testInstantiationOrderMultiGroups(): Unit = {
    val obsList = instantiate2(
      List(
        (
          "blueprint-0",
          List(newObs(SCIENCE, Instrument.GmosSouth)),
          List(
            newParameters(s"$ScienceTargetName-00", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-01", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-02", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-03", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-04", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-05", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-06", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-07", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-08", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-09", ScienceTargetCC)
          )
        ),
        (
          "blueprint-1",
          List(newObs(SCIENCE, Instrument.GmosSouth)),
          List(
            newParameters(s"$ScienceTargetName-10", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-11", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-12", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-13", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-14", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-15", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-16", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-17", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-18", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-19", ScienceTargetCC)
          )
        ),
        (
          "blueprint-2",
          List(newObs(SCIENCE, Instrument.GmosSouth)),
          List(
            newParameters(s"$ScienceTargetName-20", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-21", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-22", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-23", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-24", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-25", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-26", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-27", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-28", ScienceTargetCC),
            newParameters(s"$ScienceTargetName-29", ScienceTargetCC)
          )
        )
      )
    )

    val names = obsList.map { obs =>
      obs.findTargetObsComp.get.getTargetEnvironment.getTargets.get(0).getName
    }
    assertEquals(30, obsList.length);
    assertEquals(names.sorted, names)
  }

}
package edu.gemini.spModel.template

import edu.gemini.pot.sp.{ISPObsComponent, SPComponentType, ISPObservation, SPNodeKey}
import edu.gemini.pot.sp.SPComponentType.{INSTRUMENT_GMOSSOUTH, OBSERVER_OBSERVE, SCHEDULING_CONDITIONS, TELESCOPE_TARGETENV}
import edu.gemini.shared.util.TimeValue
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth
import edu.gemini.spModel.gemini.gmos.InstGmosSouth
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintImaging
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover.{PERCENT_50 => CC50, PERCENT_80 => CC80}
import edu.gemini.spModel.gemini.security.UserRolePrivileges
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.obsclass.ObsClass._
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.test.SpModelTestBase
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

import InstantiationFunctorTest._

class InstantiationFunctorTest extends SpModelTestBase {

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
    tfNode.setDataObject(tg)

    val tpNode = fact.createTemplateParameters(prog, WithSomeNewKey)
    val tp     = newParameters(ScienceTargetName, ScienceTargetCC)
    tpNode.setDataObject(tp)

    tgNode.addTemplateParameters(tpNode)
    tfNode.addTemplateGroup(tgNode)
    prog.setTemplateFolder(tfNode)

    // Add an observation for each of the specified obs clsses
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

  def newObs(clazz: ObsClass): ISPObservation = {
    val obsNode     = getFactory.createObservation(getProgram, WithSomeNewKey)

    obsNode.addObsComponent(getFactory.createObsComponent(getProgram, INSTRUMENT_GMOSSOUTH, WithSomeNewKey))

    val filteredChildren = obsNode.getObsComponents.asScala.filter { n =>
      n.getDataObject.getType match {
        case TELESCOPE_TARGETENV | SCHEDULING_CONDITIONS => false
        case _                                           => true
      }
    }
    obsNode.setObsComponents(filteredChildren.asJava)

    val observeNode = getFactory.createSeqComponent(getProgram, OBSERVER_OBSERVE, WithSomeNewKey)
    val observe     = new SeqRepeatObserve() <| (_.setObsClass(clazz))
    observeNode.setDataObject(observe)

    obsNode.getSeqComponent.addSeqComponent(observeNode)
    obsNode
  }

  def newObsComp(cType: SPComponentType, dob: ISPDataObject): ISPObsComponent =
    getFactory.createObsComponent(getProgram, cType, WithSomeNewKey) <| (_.setDataObject(dob))

  def newSiteQualityComponent(cc: CloudCover): ISPObsComponent =
    newObsComp(SCHEDULING_CONDITIONS, newSiteQuality(cc))

  def newTargetComponent(n: String): ISPObsComponent =
    newObsComp(TELESCOPE_TARGETENV, newTargetObsComp(n))

  def editObservation(o: ISPObservation): Unit = {
    def editObsComponent(o: ISPObservation, ct: SPComponentType, noc: ISPObsComponent): Unit =
      o.getObsComponents.asScala.find { _.getType == ct }.fold(o.addObsComponent(noc)) { oc =>
        oc.setDataObject(noc.getDataObject)
      }

    // Set conditions, target, and PA.
    editObsComponent(o, SCHEDULING_CONDITIONS, newSiteQualityComponent(EditTargetCC))
    editObsComponent(o, TELESCOPE_TARGETENV, newTargetComponent(EditTargetName))
    setPositionAngle(o, EditPA)
  }

  @Test def testDayCalGetsNeitherConditionsNorTarget(): Unit = {
    val obsList = instantiate(newObs(DAY_CAL))

    obsList match {
      case o :: Nil =>
        // Neither science site quality nor science target are copied.
        assertTrue(siteQuality(o).isEmpty && target(o).isEmpty)
      case _        =>
        fail("Expected a single observation")
    }
  }

  @Test def testReapplyDayCal(): Unit = {
    val obsList = instantiate(newObs(DAY_CAL))
    obsList.foreach(editObservation)

    reapply(obsList: _*) match {
      case o :: Nil =>
        // Everything has been reset.
        assertTrue(siteQuality(o).isEmpty && target(o).isEmpty && getPositionAngle(o) == 0.0)
      case _        =>
        fail("Exepcted a single observation")
    }

  }

  @Test def testNightCalHasScienceConditions(): Unit = {
    List(ACQ_CAL, PARTNER_CAL, PROG_CAL).foreach { c =>
      val obsList = instantiate(newObs(c))

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
      val obsList = instantiate(newObs(c))

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
      val obsList = instantiate(newObs(c))
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

object InstantiationFunctorTest {
    // The program factory interprets a null key as a request to create a new key
  val WithSomeNewKey: SPNodeKey = null

  val ScienceTargetName = "Biff"
  val EditTargetName    = "Henderson"
  val ScienceTargetCC   = CC50
  val EditTargetCC      = CC80
  val EditPA            = 42.0

  def newTarget(name: String): SPTarget =
    new SPTarget() <| (_.setName(name))

  def newTargetObsComp(name: String): TargetObsComp =
    new TargetObsComp <| (_.setTargetEnvironment(TargetEnvironment.create(newTarget(name))))

  def newSiteQuality(cc: CloudCover): SPSiteQuality =
    new SPSiteQuality() <| (_.setCloudCover(cc))

  def newParameters(targetName: String, cc: CloudCover): TemplateParameters =
    TemplateParameters.newInstance(newTarget(targetName), newSiteQuality(cc), new TimeValue(1, TimeValue.Units.hours))

  def siteQuality(o: ISPObservation): Option[SPSiteQuality] =
    dataObject[SPSiteQuality](o, SCHEDULING_CONDITIONS)

  def target(o: ISPObservation): Option[TargetObsComp] =
    dataObject[TargetObsComp](o, TELESCOPE_TARGETENV)

  def dataObject[D: Manifest](o: ISPObservation, ct: SPComponentType): Option[D] =
    obsComp[D](o, ct).map(_._2)

  def obsComp[D: Manifest](o: ISPObservation, ct: SPComponentType): Option[(ISPObsComponent, D)] =
    o.getObsComponents.asScala.find { _.getType == ct }.map(oc => (oc, oc.getDataObject)).collect {
      case (oc, d: D) => (oc, d)
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
}
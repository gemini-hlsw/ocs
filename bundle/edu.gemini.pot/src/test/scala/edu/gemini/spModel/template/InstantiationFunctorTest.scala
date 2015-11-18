package edu.gemini.spModel.template

import edu.gemini.pot.sp.{SPComponentType, ISPObservation, SPNodeKey}
import edu.gemini.pot.sp.SPComponentType.{OBSERVER_OBSERVE, SCHEDULING_CONDITIONS, TELESCOPE_TARGETENV}
import edu.gemini.shared.util.TimeValue
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintImaging
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover.{PERCENT_50 => CC50}
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.obsclass.ObsClass._
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.spModel.target.SPTarget
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

  def newObs(clazz: ObsClass): ISPObservation = {
    val obsNode     = getFactory.createObservation(getProgram, WithSomeNewKey)

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
                     target(o).exists(_.getBase.getTarget.getName == ScienceTargetName))
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
  val ScienceTargetCC   = CC50

  def newTarget(name: String): SPTarget =
    new SPTarget() <| (_.setName(name))

  def newSiteQuality(cc: CloudCover): SPSiteQuality =
    new SPSiteQuality() <| (_.setCloudCover(cc))

  def newParameters(targetName: String, cc: CloudCover): TemplateParameters =
    TemplateParameters.newInstance(newTarget(targetName), newSiteQuality(cc), new TimeValue(1, TimeValue.Units.hours))

  def siteQuality(o: ISPObservation): Option[SPSiteQuality] =
    obsComp[SPSiteQuality](o, SCHEDULING_CONDITIONS)

  def target(o: ISPObservation): Option[TargetObsComp] =
    obsComp[TargetObsComp](o, TELESCOPE_TARGETENV)

  def obsComp[D: Manifest](o: ISPObservation, ct: SPComponentType): Option[D] =
    o.getObsComponents.asScala.find { _.getType == ct }.map(_.getDataObject).collect {
      case d: D => d
    }
}
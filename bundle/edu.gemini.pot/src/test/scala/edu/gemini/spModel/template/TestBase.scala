package edu.gemini.spModel.template

import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.sp.SPComponentType.{OBSERVER_OBSERVE, SCHEDULING_CONDITIONS, TELESCOPE_TARGETENV}
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover
import edu.gemini.spModel.test.SpModelTestBase

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._
import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.seqcomp.SeqRepeatObserve

abstract class TestBase extends SpModelTestBase with TestHelp {
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
  }

  def newObs(clazz: ObsClass, inst: Instrument): ISPObservation = {
    val obsNode     = getFactory.createObservation(getProgram, inst.some, WithSomeNewKey)

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

}

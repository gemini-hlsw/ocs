package edu.gemini.too.event.service

import edu.gemini.pot.sp.ISPSeqComponent
import edu.gemini.pot.sp.SPComponentType.OBSERVER_OBSERVE
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.core.Site.GS
import edu.gemini.spModel.obs.{ObsPhase2Status, SPObservation}
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.spModel.test.SpModelTestBase
import edu.gemini.spModel.too.{Too, TooType}
import edu.gemini.too.event.api.{TooEvent, TooSubscriber}

import junit.framework.Assert._

class TestTooSubscriber extends TooSubscriber {
  var receivedEvent: Boolean = false

  override def tooObservationReady(event: TooEvent): Unit = {
    receivedEvent = true
  }
}

final class TooServiceTest extends SpModelTestBase {

  var observe: ISPSeqComponent = null
  var service: TooService      = null
  val sub: TestTooSubscriber   = new TestTooSubscriber

  override def setUp(): Unit = {
    super.setUp(SPProgramID.toProgramID("GS-2017B-T-1"))

    // Add a single observe
    observe = addSeqComponent(getObs.getSeqComponent, OBSERVER_OBSERVE)

    // Make the program a ToO program.
    Too.set(getProgram, TooType.rapid)

    // Setup TOO service.  Ordinarily this is handled by the OSGI bundle
    // activator.
    service = new TooService(getOdb, GS)
    getOdb.registerTrigger(TooCondition, service)
    getOdb.addProgramEventListener(service)

    service.addTooSubscriber(sub)
  }

  private def setObsClass(oc: ObsClass): Unit = {
    val dobj = observe.getDataObject.asInstanceOf[SeqRepeatObserve]
    dobj.setObsClass(oc)
    observe.setDataObject(dobj)
  }

  private def setPhase2Status(os: ObsPhase2Status): Unit = {
    val dataObj = getObs.getDataObject.asInstanceOf[SPObservation]
    dataObj.setPhase2Status(os)
    getObs.setDataObject(dataObj)
  }

  private def setOnHold(): Unit =
    setPhase2Status(ObsPhase2Status.ON_HOLD)

  private def setReady(): Unit =
    setPhase2Status(ObsPhase2Status.PHASE_2_COMPLETE)

  def testValidTooTrigger(): Unit = {
    setOnHold()
    Thread.sleep(500)
    assertFalse("received event when not expecting one", sub.receivedEvent)
    setReady()
    Thread.sleep(500)
    assertTrue("didn't receive event after trigger", sub.receivedEvent)
  }

  def testTooAcquisition(): Unit = {
    setObsClass(ObsClass.ACQ)
    setOnHold()
    Thread.sleep(500)
    assertFalse("received event when not expecting one", sub.receivedEvent)
    setReady()
    Thread.sleep(500)
    assertFalse("shouldn't have triggered acquisition", sub.receivedEvent)
  }

}

// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.session

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.{SPProgramID, Site}
import edu.gemini.util.security.principal.StaffPrincipal
import edu.gemini.wdba.fire.{FireAction, FireMessage, FireService}
import edu.gemini.wdba.fire.json._
import edu.gemini.wdba.glue.WdbaGlueService
import edu.gemini.wdba.glue.api.WdbaContext
import edu.gemini.wdba.test.OdbTestBase
import argonaut._
import Argonaut._
import edu.gemini.pot.sp.{SPComponentType, SPObservationID}
import edu.gemini.spModel.dataset.{Dataset, DatasetLabel}
import edu.gemini.spModel.event.ExecEvent
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.spModel.too.TooType
import edu.gemini.wdba.session.FireTest.TestEnvironment.TimeoutSec
import org.junit.Assert.assertEquals
import org.junit.Test

import java.security.Principal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import java.util.function.{BiConsumer, Consumer}

import scala.collection.mutable
import scala.concurrent.duration._

class FireTest extends OdbTestBase {

  import FireTest._

  var env: Option[TestEnvironment] =
    None

  override def setUp(): Unit = {
    super.setUp(ProgramId)

    // Add an instrument.
    addObsComponent(SPComponentType.INSTRUMENT_FLAMINGOS2)

    // Add an observe.
    val comp = addSeqComponent(getObs.getSeqComponent, SPComponentType.OBSERVER_OBSERVE)

    // Give it three steps.
    val observe = comp.getDataObject.asInstanceOf[SeqRepeatObserve]
    observe.setStepCount(3)
    comp.setDataObject(observe)

    env = Some(TestEnvironment.initializeForDatabase(getOdb))
    env.foreach(_.start())
  }

  override def tearDown(): Unit = {
    env.foreach(_.stop())
    super.tearDown()
  }

  private def doTest(expectedMessages: List[FireMessage])(test: TestEnvironment => Unit): Unit = {
    def toJsonString[A: EncodeJson](list: List[A]): String =
      Json.array(list.map(_.asJson): _*).spaces2

    val expected = toJsonString(expectedMessages)

    env.foreach { e =>
      test(e)
      val actual = toJsonString(e.buf.toList.map(FireMessage.uuid.set(Uuid)))
      assertEquals("FireMessage comparison", expected, actual)
    }
  }

  @Test def testSequence(): Unit = {
    val expected = mutable.Buffer(
      // Inexplicably, we subtract a second from the visit start time.
      StartVisitMessage,
      initMessage("Slew"),
      initMessage("Start Sequence"),
      FireMessage.completedStepCount.set(1)
        .andThen(FireMessage.datasets.set(Datasets))
        .apply(initMessage("Start Dataset")),
      FireMessage.completedStepCount.set(1)
        .andThen(FireMessage.datasets.set(Datasets))
        .apply(initMessage("End Dataset")),
      FireMessage.completedStepCount.set(1)
        .apply(initMessage("End Sequence"))
    )

    val obsId = getObs.getObservationID.stringValue

    doTest(expected.toList) { e =>
      e.man.observationStart(e.sid, obsId, When)
      e.processOne() // start visit
      e.processOne() // slew
      e.man.sequenceStart(e.sid, obsId, When, "IGNORED")
      e.processOne()
      e.man.datasetStart(e.sid, obsId, When, s"$obsId-001", Filename)
      e.processOne()
      e.man.datasetComplete(e.sid, obsId, When, s"$obsId-001", Filename)
      e.processOne()
      e.man.sequenceEnd(e.sid, obsId, When)
      e.processOne()
    }
  }

}

object FireTest {

  val ProgramId: SPProgramID =
    SPProgramID.toProgramID("GS-2021B-Q-9999")

  val ObservationId: SPObservationID =
    new SPObservationID(ProgramId, 1)

  val User: java.util.Set[Principal] =
    java.util.Collections.singleton(StaffPrincipal.Gemini)

  val When: Instant =
    Instant.ofEpochMilli(1628863911712L)

  val Uuid: UUID =
    UUID.fromString("aa3dd6d6-3322-418a-a665-a9d8810bff3d")

  val Filename: String =
    "S20210813S0001.fits"

  val Dataset: Dataset =
    new Dataset(new DatasetLabel(ObservationId, 1), Filename, When.toEpochMilli)

  val Datasets: List[Dataset] =
    List(Dataset)

  def initMessage(nature: String): FireMessage =
    FireMessage.visitStart.set(Some(When.minusMillis(1L))) // Inexplicably, we subtract a second from the visit start time
      .andThen(FireMessage.observationId.set(Some(ObservationId)))
      .andThen(FireMessage.too.set(Some(TooType.none)))
      .andThen(FireMessage.duration.set(345.seconds))
      .andThen(FireMessage.totalStepCount.set(3))
      .apply(FireMessage.empty(Uuid, When, nature))

  val StartVisitMessage: FireMessage =
      FireMessage.time.modify(_.minusMillis(1)) // Inexplicably, we subtract a second from the visit start time
        .apply(initMessage("Start Visit"))

  final case class TestEnvironment(
    que: LinkedBlockingQueue[FireMessage],
    ctx: WdbaContext,
    man: SessionManagement,
    sid: String,
    dup: DBUpdateService,
    fyr: FireService
  ) {

    val buf: mutable.Buffer[FireMessage] =
      mutable.Buffer.empty

    def start(): Unit = {
      fyr.start()
      dup.start()
    }

    def stop(): Unit = {
      dup.stop()
      fyr.stop()
    }

    // Wait for the expected FireMessage to appear and then put it in the
    // result buffer to check later.
    def processOne(): Unit =
      Option(que.poll(TimeoutSec, TimeUnit.SECONDS)).foreach(buf.append(_))

  }

  object TestEnvironment {

    val TimeoutSec: Int = 1

    def initializeForDatabase(db: IDBDatabaseService): TestEnvironment = {
      val dac = new WdbaGlueService(db, User)
      val ctx = new WdbaContext(Site.GS, dac, User)
      val dup = new DBUpdateService(ctx)
      val que = new LinkedBlockingQueue[FireMessage]
      val fyr = new FireService(db, m => FireAction(que.offer(m, TimeoutSec, TimeUnit.SECONDS)))
      val cns = new Consumer[ExecEvent] {
        override def accept(e: ExecEvent): Unit =
          dup.handleEvent(e).whenComplete(new BiConsumer[ExecEvent, Throwable] {
            override def accept(e: ExecEvent, u: Throwable): Unit =
              Option(e).foreach(fyr.addEvent)
          })
      }
      val man = new SessionManagement(ctx, cns)
      val sid = man.createSession()

      TestEnvironment(que, ctx, man, sid, dup, fyr)
    }
  }

}

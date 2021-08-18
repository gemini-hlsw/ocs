// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.session

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.{SPProgramID, Site}
import edu.gemini.util.security.principal.StaffPrincipal
import edu.gemini.wdba.fire.FireService
import edu.gemini.wdba.glue.WdbaGlueService
import edu.gemini.wdba.glue.api.WdbaContext
import edu.gemini.wdba.test.OdbTestBase
import argonaut._
import edu.gemini.spModel.event.ExecEvent
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test

import java.security.Principal
import java.util.function.{BiConsumer, Consumer}

class FireTest extends OdbTestBase {

  import FireTest._

  var env: Option[TestEnvironment] =
    None

  override def setUp(): Unit = {
    super.setUp(ProgramId)

    env = Some(TestEnvironment(getOdb))
    env.foreach(_.start())
  }

  override def tearDown(): Unit = {
    env.foreach(_.stop())
    super.tearDown()
  }

  def doTest[A](jsonString: String)(test: TestEnvironment => Unit): Unit = {
    val expected = Parse.parse(jsonString).fold(identity, _.spaces2)

    env.foreach { e =>
      assertTrue(e.man.addObservation(e.sid, getObs.getObservationID.stringValue))
      test(e)
      Thread.sleep(500)
      assertEquals("FireMessage comparison", expected, e.buf.toString)
    }
  }

  @Test def testFoo(): Unit = {
    val expected =
      """
        {
        }
      """

    doTest(expected) { e =>
      e.man.observationStart(e.sid, getObs.getObservationID.stringValue)
    }
  }

}

object FireTest {

  val ProgramId: SPProgramID =
    SPProgramID.toProgramID("GS-2021B-Q-9999")

  val user: java.util.Set[Principal] =
    java.util.Collections.singleton(StaffPrincipal.Gemini)

  final case class TestEnvironment(
    buf: StringBuilder,
    ctx: WdbaContext,
    man: SessionManagement,
    sid: String,
    dup: DBUpdateService,
    fyr: FireService
  ) {

    def start(): Unit = {
      fyr.start()
      dup.start()
    }

    def stop(): Unit = {
      dup.stop()
      fyr.stop()
    }
  }

  object TestEnvironment {

    def apply(db: IDBDatabaseService): TestEnvironment = {
      val buf = StringBuilder.newBuilder
      val dac = new WdbaGlueService(db, user)
      val ctx = new WdbaContext(Site.GS, dac, user)
      val dup = new DBUpdateService(ctx)
      val fyr = FireService.forTesting(db, buf)
      val cns = new Consumer[ExecEvent] {
        override def accept(e: ExecEvent): Unit =
          dup.handleEvent(e).whenComplete(new BiConsumer[ExecEvent, Throwable] {
            override def accept(e: ExecEvent, u: Throwable): Unit =
              Option(e).foreach(fyr.addEvent)
          })
      }
      val man = new SessionManagement(ctx, cns)
      val sid = man.createSession()

      TestEnvironment(buf, ctx, man, sid, dup, fyr)
    }
  }

}

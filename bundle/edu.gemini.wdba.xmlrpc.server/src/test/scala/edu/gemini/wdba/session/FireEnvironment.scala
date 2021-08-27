// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.session

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.{SPProgramID, Site}
import edu.gemini.spModel.dataset.{Dataset, DatasetLabel}
import edu.gemini.spModel.event.ExecEvent
import edu.gemini.util.security.principal.StaffPrincipal
import edu.gemini.wdba.fire.{FireAction, FireMessage, FireService}
import edu.gemini.wdba.glue.WdbaGlueService
import edu.gemini.wdba.glue.api.WdbaContext

import java.security.Principal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import java.util.function.{BiConsumer, Consumer}

import scala.collection.mutable

final case class FireEnvironment(
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
    Option(que.poll(FireEnvironment.TimeoutSec, TimeUnit.SECONDS)).foreach(buf.append(_))

}

object FireEnvironment {

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

  val TimeoutSec: Int =
    1

  def initializeForDatabase(
    db: IDBDatabaseService
  )(
    action: LinkedBlockingQueue[FireMessage] => FireMessage => FireAction[Unit]
  ): FireEnvironment = {

    val dac = new WdbaGlueService(db, User)
    val ctx = new WdbaContext(Site.GS, dac, User)
    val dup = new DBUpdateService(ctx)
    val que = new LinkedBlockingQueue[FireMessage]
    val fyr = new FireService(db, action(que))
    val cns = new Consumer[ExecEvent] {
      override def accept(e: ExecEvent): Unit =
        dup.handleEvent(e).whenComplete(new BiConsumer[ExecEvent, Throwable] {
          override def accept(e: ExecEvent, u: Throwable): Unit =
            Option(e).foreach(fyr.handleEvent)
        })
    }
    val man = new SessionManagement(ctx, cns)
    val sid = man.createSession()

    FireEnvironment(que, ctx, man, sid, dup, fyr)
  }
}

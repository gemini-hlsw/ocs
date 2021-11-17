// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.session

import argonaut._
import Argonaut._
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import edu.gemini.wdba.fire.{FireMessage, FirePost}
import edu.gemini.wdba.fire.json._
import edu.gemini.wdba.test.OdbTestBase
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.spModel.too.TooType
import edu.gemini.wdba.session.FireEnvironment._
import org.junit.Assert.assertEquals
import org.junit.Test

import java.net.{InetSocketAddress, URL}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import scala.collection.mutable
import scala.concurrent.duration._
import scalaz._
import Scalaz._

class FireTest extends OdbTestBase {

  import FireTest._

  var env: Option[FireEnvironment] =
    None

  var server: Option[HttpServer] =
    Option.empty

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

    // To skip the post, this environment can be used instead
//    env = Some(FireEnvironment.initializeForDatabase(getOdb) { que =>
//      m => FireAction(que.offer(m, TimeoutSec, TimeUnit.SECONDS))
//    })

    server = Some(HttpServer.create(new InetSocketAddress(0), 0))

    env = Some(FireEnvironment.initializeForDatabase(getOdb) { que =>

      val handler = new FireHandler(que)

      server.foreach { s =>
        s.createContext("/fire", handler)
        s.setExecutor(null)
        s.start()
      }

      val url = server.map(s => new URL(s"http://localhost:${s.getAddress.getPort}/fire")).get

      m => FirePost.post[FireMessage](url, m).void
    })

    env.foreach(_.start())
  }

  override def tearDown(): Unit = {
    server.foreach(_.stop(0))
    env.foreach(_.stop())
    super.tearDown()
  }

  private def doTest(expectedMessages: List[FireMessage])(test: FireEnvironment => Unit): Unit = {
    def toJsonString[A: EncodeJson](list: List[A]): String =
      Json.array(list.map(_.asJson): _*).spaces2

    val expected = toJsonString(expectedMessages)

    env.foreach { e =>
      test(e)
      val actual  = toJsonString(e.buf.toList.map(FireMessage.uuid.set(Uuid)))
      val message =
        s"""
           |FireMessage comparison failure.
           |
           |Expected:
           |$expected
           |
           |--
           |
           |Actual:
           |$actual
           |
           |""".stripMargin
      assertEquals(message, expected, actual)
    }
  }

  @Test def testSequence(): Unit = {
    val expected = mutable.Buffer(
      // Inexplicably, we subtract a second from the visit start time.
      StartVisitMessage,
      initMessage("Slew"),
      initMessage("Start Sequence"),
      FireMessage.completedStepCount.set(1)
        .andThen(FireMessage.fileNames.set(Datasets.map(_.getDhsFilename)))
        .apply(initMessage("Start Dataset")),
      FireMessage.completedStepCount.set(1)
        .andThen(FireMessage.fileNames.set(Datasets.map(_.getDhsFilename)))
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

  final class FireHandler(
    queue: LinkedBlockingQueue[FireMessage]
  ) extends HttpHandler {

    private def unsafeHandle(ex: HttpExchange): Unit = {
      val jsonString = scala.io.Source.fromInputStream(ex.getRequestBody).mkString
      Parse.decodeEither[FireMessage](jsonString) match {
        case Left(err)  =>
          println(s"FireMessage parsing failed: $err")
          println(jsonString)
        case Right(msg) =>
          queue.offer(msg, TimeoutSec, TimeUnit.SECONDS)
      }

      val response = "{}"
      ex.sendResponseHeaders(200, response.length)
      val os = ex.getResponseBody
      try {
        os.write(response.getBytes("UTF-8"))
      } finally {
        os.close()
      }
    }

    override def handle(ex: HttpExchange): Unit =
      try {
        unsafeHandle(ex)
      } finally {
        ex.close()
      }
  }

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

}

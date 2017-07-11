package edu.gemini.seqexec.odb

import edu.gemini.pot.sp.{ISPObservation, SPObservationID}
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.seqexec.odb.SeqFailure.{MissingObservation, SeqException}

import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances.IDENTITY_MAP
import edu.gemini.spModel.config2.ConfigSequence
import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.io.SpImportService
import java.io.{BufferedReader, InputStreamReader, StringReader}
import java.net.{HttpURLConnection, URL}
import java.net.HttpURLConnection.{HTTP_NOT_FOUND, HTTP_OK}
import java.time.Duration
import java.util.stream.Collectors

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
 * Sequence Executor Service API.
 */
trait SeqExecService {

  /** Fetches the sequence associated with the given observation id, if it
    * exists. */
  def sequence(oid: SPObservationID): TrySeq[ConfigSequence]
}

object SeqExecService {
  val ConnectTimeout: Duration =
    Duration.ofSeconds(20)

  val ReadTimeout: Duration =
    Duration.ofSeconds(0)  // 0 is "infinite"

  private def url(peer: Peer, oid: SPObservationID): TrySeq[URL] =
    catchingAll {
      new URL(s"http://${peer.host}:8442/ocs3/fetch/pio/${oid.stringValue}")
    }

  private def open(url: URL): TrySeq[HttpURLConnection] =
    catchingAll {
      val conn = url.openConnection.asInstanceOf[HttpURLConnection]
      conn.setConnectTimeout(ConnectTimeout.toMillis.toInt)
      conn.setDoOutput(false)
      conn.setDoInput(true)
      conn.setReadTimeout(ReadTimeout.toMillis.toInt)
      conn
    }

  private def read(conn: HttpURLConnection, oid: SPObservationID): TrySeq[String] =
    trySeq {
      conn.getResponseCode match {
        case HTTP_OK        =>
          closing(new BufferedReader(new InputStreamReader(conn.getInputStream))) {
            _.lines.collect(Collectors.joining("\n")).right
          }

        case HTTP_NOT_FOUND =>
          MissingObservation(oid).left

        case x              =>
          val msg = s"Unexpected response code: $x${Option(conn.getResponseMessage).map(m => s": $m").orZero}"
          SeqException(new RuntimeException(msg)).left
      }
    }

  private def parse(xml: String): TrySeq[ISPObservation] =
    trySeq {
      val db = DBLocalDatabase.createTransient
      try {
        val is = new SpImportService(db)
        is.importProgramXml(new StringReader(xml)) match {
          case scala.util.Success(p) => p.getAllObservations.asScala.head.right
          case scala.util.Failure(e) => SeqException(e).left
        }
      } finally {
        db.getDBAdmin.shutdown
      }
    }

  private def extractSequence(obs: ISPObservation): TrySeq[ConfigSequence] =
    catchingAll {
      ConfigBridge.extractSequence(obs, null, IDENTITY_MAP, true)
    }

  /** Constructs a SeqExecService that works with a servlet running in the ODB.
    * When a sequence is requested, it contacts the servlet and requests the
    * observation XML (wrapped in a program shell suitable for importing). The
    * XML is then parsed and the sequence is extracted.
    */
  def client(peer: Peer): SeqExecService =
    new SeqExecService {
      override def sequence(oid: SPObservationID): TrySeq[ConfigSequence] =
        for {
          u <- url(peer, oid)
          c <- open(u)
          x <- read(c, oid)
          o <- parse(x)
          s <- extractSequence(o)
        } yield s
    }
}
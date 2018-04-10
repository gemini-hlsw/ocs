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
import java.time.{Duration, Instant}
import java.util.stream.Collectors

import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obslog.ObsLog

import scala.collection.JavaConverters._
import scala.collection.breakOut

case class ExecutedDataset(timestamp: Instant, filename: String)
case class SeqexecSequence(title: String, datasets: Map[Int, ExecutedDataset], config: ConfigSequence)

/**
 * Sequence Executor Service API.
 */
trait SeqExecService {

  /** Fetches the sequence associated with the given observation id, if it
    * exists. */
  def sequence(oid: SPObservationID): TrySeq[SeqexecSequence]
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
          closing(new BufferedReader(new InputStreamReader(conn.getInputStream))) { r =>
            Right(r.lines.collect(Collectors.joining("\n")))
          }

        case HTTP_NOT_FOUND =>
          Left(MissingObservation(oid))

        case x              =>
          val msg = s"Unexpected response code: $x${Option(conn.getResponseMessage).map(m => s": $m").getOrElse("")}"
          Left(SeqException(new RuntimeException(msg)))
      }
    }

  private def parse(xml: String): TrySeq[ISPObservation] =
    trySeq {
      val db = DBLocalDatabase.createTransient
      try {
        val is = new SpImportService(db)
        is.importProgramXml(new StringReader(xml)) match {
          case scala.util.Success(p) => Right(p.getAllObservations.asScala.head)
          case scala.util.Failure(e) => Left(SeqException(e))
        }
      } finally {
        db.getDBAdmin.shutdown
      }
    }

  private def extractName(obs: ISPObservation): TrySeq[String] = trySeq {
    Right(obs.getDataObject.getTitle)
  }

  private def extractSequence(obs: ISPObservation): TrySeq[ConfigSequence] =
    catchingAll {
      ConfigBridge.extractSequence(obs, null, IDENTITY_MAP, true)
    }

  private def extractExecutedDatsets(obs: ISPObservation): TrySeq[Map[Int, ExecutedDataset]] =
    catchingAll {
      Option(ObsLog.getIfExists(obs)) match {
        case Some(obsLog) => obsLog.getAllDatasetRecords.asScala.map(_.exec.dataset).map { d =>
          d.getIndex -> ExecutedDataset(Instant.ofEpochMilli(d.getTimestamp), d.getDhsFilename)
        }(breakOut)
        case None         => throw new RuntimeException(s"Observation ${obs.getObservationID} not found")
      }
    }

  /** Constructs a SeqExecService that works with a servlet running in the ODB.
    * When a sequence is requested, it contacts the servlet and requests the
    * observation XML (wrapped in a program shell suitable for importing). The
    * XML is then parsed and the sequence is extracted.
    */
  def client(peer: Peer): SeqExecService =
    new SeqExecService {
      override def sequence(oid: SPObservationID): TrySeq[SeqexecSequence] =
        for {
          u <- url(peer, oid).right
          c <- open(u).right
          x <- read(c, oid).right
          o <- parse(x).right
          s <- extractSequence(o).right
          n <- extractName(o).right
          e <- extractExecutedDatsets(o).right
        } yield SeqexecSequence(n, e, s)
    }
}

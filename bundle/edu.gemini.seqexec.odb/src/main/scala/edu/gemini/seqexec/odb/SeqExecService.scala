package edu.gemini.seqexec.odb

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.seqexec.odb.SeqFailure.SeqException

import edu.gemini.spModel.config2.ConfigSequence
import edu.gemini.spModel.core.Peer

import java.io.{BufferedReader, InputStreamReader}
import java.net.{HttpURLConnection, URL}
import java.net.HttpURLConnection.{HTTP_NOT_FOUND, HTTP_OK}
import java.time.Duration
import java.util.stream.Collectors

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
    try {
      conn.getResponseCode match {
        case HTTP_OK        =>
          closing(new BufferedReader(new InputStreamReader(conn.getInputStream))) {
            _.lines.collect(Collectors.joining("\n")).right
          }

        case HTTP_NOT_FOUND =>
          SeqFailure.MissingObservation(oid).left

        case x              =>
          val msg = s"Unexpected response code: $x${Option(conn.getResponseMessage).map(m => s": $m").orZero}"
          SeqFailure.SeqException(new RuntimeException(msg)).left
      }
    } catch {
      case ex: Exception => SeqFailure.SeqException(ex).left
    }


  def client(peer: Peer): SeqExecService = new SeqExecService {
    override def sequence(oid: SPObservationID): TrySeq[ConfigSequence] = {
      ???
    }
  }
}
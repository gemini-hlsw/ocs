package edu.gemini.model.p1.submit

import java.security.KeyStore
import javax.net.ssl.{TrustManagerFactory, SSLContext, HttpsURLConnection}

import edu.gemini.model.p1.immutable.Proposal
import edu.gemini.model.p1.submit.SubmitResult.{SubmitException, Offline, ServiceError}
import ResponseParser.parse

import java.io.{InputStream, IOException}
import java.net.{HttpURLConnection, URL}
import java.net.HttpURLConnection.HTTP_CREATED
import java.util.logging.{Logger, Level}

import scala.actors.Futures._
import scala.io.Source
import scalaz._
import Scalaz._

case class FutureSubmission(dest: SubmitDestination, url: String, proposal: Proposal) {

  private val LOG = Logger.getLogger(classOf[FutureSubmission].getName)

  private val fut = future { sendSynchronously }

  lazy val result = DestinationSubmitResult(dest, fut())

  val timeout = 1 * 60000  // 1 min ?

  private val errorAdapter:PartialFunction[SubmitResult, SubmitResult] = {
    case ServiceError(None, _, code, msg) => ServiceError(dest.some, proposal.proposalClass.some, code, msg)
    case Offline(None)                    => Offline(dest.some)
    case r                                => r
  }

  private def sendSynchronously: SubmitResult =
    try {
      val http = new URL(url).openConnection().asInstanceOf[HttpsURLConnection]

      http.setSSLSocketFactory(FutureSubmission.sslFactory)
      http.setReadTimeout(timeout)
      http.setRequestProperty("Accept-Charset", "UTF-8")

      Post.post(http, proposal) match {
        case Left(failure) => errorAdapter(failure)
        case Right(())     => processResponse(http)
      }
    } catch {
      case e: IOException =>
        LOG.log(Level.INFO, "IO Exception while posting.", e)
        Offline(Some(dest))
      case t: Throwable   =>
        LOG.log(Level.INFO, "Exception while posting.", t)
        SubmitException(dest.some, t)
    }

  private def processResponse(http: HttpURLConnection): SubmitResult = {
    def read(is: InputStream): String = {
      val s = Source.fromInputStream(is, "UTF-8")
      try { s.mkString } finally { s.close() }
    }

    http.getResponseCode match {
      case HTTP_CREATED => parse(read(http.getInputStream))
      case _            =>
        Option(http.getErrorStream).map(s => parse(read(s))).collect(errorAdapter).getOrElse {
          ServiceError(dest.some, proposal.proposalClass.some, 0, "An unexpected error happened in the submission server.")
        }
    }
  }

}

object FutureSubmission {

  val sslFactory = {
    // Load keystore from the bundle
    val in = this.getClass.getResourceAsStream("/phase1.jks")
    // Password in the clear but the keystore has only public keys
    val ks = KeyStore.getInstance("JKS") <| {_.load(in, "phase1".toCharArray)}

    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm) <| {_.init(ks)}
    // Socket factory to connect to the phase1 backend and complete the SSL handshake with the public certificates
    SSLContext.getInstance("TLS") <| {_.init(null, tmf.getTrustManagers, null)} |> {_.getSocketFactory}
  }
}
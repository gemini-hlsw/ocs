package edu.gemini.gsa.query

import edu.gemini.spModel.core.catchingNonFatal
import edu.gemini.gsa.query.GsaQueryError._
import argonaut._
import Argonaut._
import java.io.{IOException, InputStream}
import java.net.{HttpURLConnection, URL}
import java.net.HttpURLConnection._
import java.time.{Duration, Instant}
import java.util.logging.{Level, Logger}

import scala.Function.const
import scala.io.Source
import scalaz._
import Scalaz._
import edu.gemini.util.ssl.GemSslSocketFactory
import javax.net.ssl.HttpsURLConnection

/** GsaQuery handles the grim procedural details of making get and post
  * requests to the GSA server with an HttpURLConnection.
  *
  * Someday, we'd like to use the http4s client code to do this type of
  * thing. */
private [query] object GsaQuery {
  private val Log      = Logger.getLogger(GsaRecordQuery.getClass.getName)
  private val Cset     = "UTF-8"

  val ConnectTimeout   =  30000
  val ReadTimeout      = 120000

  def get[A : DecodeJson](url: URL): GsaResponse[A] =
    query[A](url)(const(()))

  def post[A : EncodeJson, B : DecodeJson](url: URL, a: A, auth: GsaAuth): GsaResponse[B] =
    query[B](url) { con =>
      con.setDoOutput(true)
      con.setInstanceFollowRedirects(false)
      con.setUseCaches(false)

      con.setRequestMethod("POST")
      con.setRequestProperty("Content-Type", s"application/json;charset=$Cset")
      con.setRequestProperty("Cookie",       s"gemini_api_authorization=${auth.value}")

      val json = a.asJson.spaces2
      logIf(JsonLevel) { s"GSA QA state update post:\n$json" }
      val data = json.getBytes(Cset)
      con.setRequestProperty("Content-Length", data.length.toString)
      con.setFixedLengthStreamingMode(data.length)

      val os = con.getOutputStream
      try { con.getOutputStream.write(data) } finally { os.close() }
    }

  private def query[A : DecodeJson](url: URL)(prep: HttpURLConnection => Unit): GsaResponse[A] = {
    logIf(DetailLevel) { s"Start GSA query: ${url.toString}" }

    val startTime = Instant.now()
    val result    = catchingNonFatal { unsafeDoQuery[A](url, prep) }.fold({
      case io: IOException => IoException(io).left
      case t: Throwable    => Unexpected(t).left
    }, identity)

    if (Log.isLoggable(DetailLevel)) {
      val duration       = Duration.between(startTime, Instant.now())
      val (message, ex)  = result match {
        case \/-(_)     => (s"Retrieved result from GSA.", none[Throwable])
        case -\/(error) => (error.explain, error.exception)
      }
      Log.log(DetailLevel, s"End GSA query (${duration.toMillis} ms) ${url.toString}. " + message, ex.orNull)
    }

    result
  }

  private def unsafeDoQuery[A : DecodeJson](url: URL, prep: HttpURLConnection => Unit): GsaResponse[A] = {
    def read(is: InputStream): String = {
      val s = Source.fromInputStream(is, Cset)
      try { s.mkString } finally { s.close() }
    }

    val con = url.openConnection() match {
      // Order is important as HttpsURLConnection extends HttpURLConnection
      case con: HttpsURLConnection =>
        con.setSSLSocketFactory(GemSslSocketFactory.get)
        con
      case con: HttpURLConnection =>
        con
    }
    con.setConnectTimeout(ConnectTimeout)
    con.setReadTimeout(ReadTimeout)
    con.setRequestProperty("Accept-Charset", Cset)

    prep(con)

    try {
      con.getResponseCode match {
        case HTTP_OK   =>
          val s = read(con.getInputStream)
          logIf(JsonLevel) { s"GSA response ($url):\n$s" }
          \/.fromEither(Parse.decodeEither[A](s).leftMap { _ =>
            InvalidResponse(s"Could not parse GSA server response:\n$s")
          })
        case errorCode =>
          RequestError(errorCode, read(con.getErrorStream)).left
      }
    } finally {
      con.disconnect()
    }
  }

  private def logIf(level: Level)(s: => String): Unit =
    if (Log.isLoggable(level)) {
      Log.log(level, s)
    }
}

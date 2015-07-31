package edu.gemini.ags.client.impl

import edu.gemini.model.p1.immutable.Observation
import edu.gemini.ags.client.api._
import edu.gemini.util.ssl.GemSslSocketFactory
import java.net.{URLConnection, HttpURLConnection, URL}
import HttpURLConnection.HTTP_OK
import io.Source

import java.io.{InputStream, IOException}
import javax.net.ssl.{SSLSession, HostnameVerifier, HttpsURLConnection}
import java.util.logging.{Level, Logger}

/**
 * AGS client that connects to a remote AGS service to obtain estimates.
 */
object AgsHttpClient {
  private val Log = Logger.getLogger(getClass.getName)
  private val hostnameVerifier: HostnameVerifier = new HostnameVerifier {
     def verify(s: String, sslSession: SSLSession) = true
  }
}

import AgsHttpClient.{Log, hostnameVerifier}

case class AgsHttpClient(host: String, port: Int) extends AgsClient {
  val timeout = 3 * 60000  // 1 min ?
  val qurl = QueryUrl(host, port)

  def url(obs: Observation, time: Long): Option[URL] =
    qurl.format(obs, time).right.toOption

  def estimateNow(obs: Observation, time: Long): AgsResult =
    qurl.format(obs, time) match {
      case Left(msg)  => AgsResult.Incomplete(msg)
      case Right(url) => estimateNow(url)
    }

  private def estimateNow(url: URL): AgsResult = {
    try {
      AgsHttpClient.Log.info(s"AGS Query to $url")
      val conn = url.openConnection().asInstanceOf[HttpsURLConnection]
      conn.setHostnameVerifier(hostnameVerifier)
      conn.setSSLSocketFactory(GemSslSocketFactory.get)
      conn.setReadTimeout(timeout)
      Charset.set(conn)
      Response(conn).result
    } catch {
      case io: IOException =>
        Log.log(Level.INFO, "I/O Exception while fetching AGS estimate, presumed offline", io)
        AgsResult.Offline
      case t: Throwable   =>
        Log.log(Level.WARNING, "Exception while fetching AGS estimate", t)
        AgsResult.Error(t)
    }
  }
}

private object Charset {
  val default = "UTF-8"

  def set(conn: URLConnection) {
    conn.setRequestProperty("Accept-Charset", default)
  }

  def get(conn: URLConnection): String =
    Option(conn.getContentType).getOrElse("").replace(" ", "").split(';') find {
      _.startsWith("charset=")
    } map {
      _.substring("charset=".length)
    } getOrElse default
}

private case class Response(code: Int, msg: String) {
  val isSuccess     = (code >= 200) && (code < 300)
  val isClientError = (code >= 400) && (code < 500)

  val result =
    if (isSuccess)
      AgsResult(msg)
    else if (isClientError)
      AgsResult.Incompatible(code, msg)
    else
      AgsResult.ServiceError(code, msg)
}

private object Response {
  def apply(conn: HttpURLConnection): Response = {
    def read(is: HttpURLConnection => InputStream): String = {
      val s = Source.fromInputStream(is(conn), Charset.get(conn))
      try { s.mkString } finally { s.close() }
    }

    conn.getResponseCode match {
      case HTTP_OK => Response(HTTP_OK, read(_.getInputStream))
      case code    => Response(code,    read(_.getErrorStream))
    }
  }
}

package edu.gemini.ags.client.impl

import edu.gemini.ags.client.api._
import edu.gemini.model.p1.immutable.Observation
import edu.gemini.util.ssl.GemSslSocketFactory
import java.net.{HttpURLConnection, URL, URLConnection}
import HttpURLConnection.HTTP_OK

import io.Source
import java.io.{IOException, InputStream}
import java.util.Collections
import java.util.{LinkedHashMap, Map}
import javax.net.ssl.{HostnameVerifier, HttpsURLConnection, SSLSession}
import java.util.logging.{Level, Logger}

import edu.gemini.ags.client.api.AgsResult.Success

import scalaz._
import Scalaz._

/**
 * AGS client that connects to a remote AGS service to obtain estimates.
 */
object AgsHttpClient {
  private val Log = Logger.getLogger(getClass.getName)
  private val hostnameVerifier: HostnameVerifier = new HostnameVerifier {
     def verify(s: String, sslSession: SSLSession) = true
  }

  // LRU cache to minimize lookups.
  val lRUCache = {
    val CacheLimit = 1000
    val cache = new LinkedHashMap[String, AgsResult.Success](CacheLimit, 0.75f, true) {
      override def removeEldestEntry(eldest: Map.Entry[String, Success]): Boolean =
        size() > CacheLimit
    }
    Collections.synchronizedMap(cache)
  }
}

import AgsHttpClient.{Log, hostnameVerifier, lRUCache}

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
      Option(lRUCache.get(url.toString) : AgsResult) | {
        val conn = url.openConnection().asInstanceOf[HttpsURLConnection]
        conn.setHostnameVerifier(hostnameVerifier)
        conn.setSSLSocketFactory(GemSslSocketFactory.get)
        conn.setReadTimeout(timeout)
        Charset.set(conn)

        val result = Response(conn).result
        result match {
          case s:AgsResult.Success => lRUCache.put(url.toString, s)
          case _                   =>
        }
        result
      }
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

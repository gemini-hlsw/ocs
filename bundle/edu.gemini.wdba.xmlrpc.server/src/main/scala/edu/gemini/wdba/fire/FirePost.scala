// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.fire

import argonaut.{EncodeJson, Json, Parse}
import argonaut.JsonIdentity._

import java.io.InputStream
import java.net.{HttpURLConnection, URL}
import java.net.HttpURLConnection._
import java.time.{Duration, Instant}
import java.util.logging.{Level, Logger}

import scala.io.Source
import scala.concurrent.duration._
import scalaz._
import Scalaz._
import scalaz.concurrent.Task

object FirePost {
  private val Log: Logger  = Logger.getLogger(FirePost.getClass.getName)
  private val Cset: String = "UTF-8"

  val ConnectTimeout: FiniteDuration = 30.seconds
  val ReadTimeout: FiniteDuration    =  2.minutes

  def post[A : EncodeJson](url: URL, a: A): FireAction[Json] =
    query(url) { con =>
      con.setDoOutput(true)
      con.setInstanceFollowRedirects(false)
      con.setUseCaches(false)

      con.setRequestMethod("POST")
      con.setRequestProperty("Content-Type", s"application/json;charset=$Cset")

      val json = a.asJson.spaces2
      logIf(DetailLevel) { s"FireMessage post:\n$json" }
      val data = json.getBytes(Cset)
      con.setRequestProperty("Content-Length", data.length.toString)
      con.setFixedLengthStreamingMode(data.length)

      val os = con.getOutputStream
      try { con.getOutputStream.write(data) } finally { os.close() }
    }

  private def query(url: URL)(prep: HttpURLConnection => Unit): FireAction[Json] =
    for {
      fra <- FireAction.catching(unsafeDoLoggedQuery(url, prep))
      a   <- EitherT.either[Task, FireFailure, Json](fra)
    } yield a

  private def unsafeDoLoggedQuery(url: URL, prep: HttpURLConnection => Unit): FireResponse[Json] = {
    logIf(DetailLevel) { s"Start Fire message POST to ${url.toString}" }

    val startTime = Instant.now()
    val result = unsafeDoQuery(url, prep)

    if (Log.isLoggable(DetailLevel)) {
      val duration       = Duration.between(startTime, Instant.now())
      val (message, ex)  = result match {
        case \/-(_)     => (s"Retrieved result from Fire server.", none[Throwable])
        case -\/(error) => (error.message, error.exception)
      }
      Log.log(DetailLevel, s"End Fire post (${duration.toMillis} ms) ${url.toString}. " + message, ex.orNull)
    }

    result
  }

  private def unsafeDoQuery(url: URL, prep: HttpURLConnection => Unit): FireResponse[Json] = {
    def read(is: InputStream): String = {
      val s = Source.fromInputStream(is, Cset)
      try { s.mkString } finally { s.close() }
    }

    val con = url.openConnection() match {
      case c: HttpURLConnection =>
        c
      case c                    =>
        throw new RuntimeException("Expected HttpURLConnection: " + c)
    }
    con.setConnectTimeout(ConnectTimeout.toMillis.toInt)
    con.setReadTimeout(ReadTimeout.toMillis.toInt)
    con.setRequestProperty("Accept-Charset", Cset)

    prep(con)

    try {
      con.getResponseCode match {
        case HTTP_OK   =>
          val s = read(con.getInputStream)
          logIf(DetailLevel) { s"Fire response ($url):\n$s" }
          \/.fromEither(Parse.parse(s).leftMap { e =>
            FireFailure.invalidResponse(s"Could not parse Fire server response: $e\n$s")
          })

        case errorCode =>
          FireFailure.postError(errorCode, read(con.getErrorStream)).left
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

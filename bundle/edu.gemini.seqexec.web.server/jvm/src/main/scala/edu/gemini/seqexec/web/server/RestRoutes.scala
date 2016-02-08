package edu.gemini.seqexec.web.server

import edu.gemini.seqexec.web.common.Comment
import org.http4s.{UrlForm, HttpService}
import org.http4s.dsl._

import upickle.default._

import scalaz._
import Scalaz._

object RestRoutes {
  // Local in memory comments list
  val comments = List.empty[Comment]

  val parse: String => Throwable \/ Comment =
      json => \/.fromTryCatchNonFatal { read[Comment](json) }

  val service = HttpService {
    case req @ GET -> Root / "comments" =>
      Ok(write(comments))
    case req @ POST -> Root / "comments" =>
      Ok(write(comments))
  }
}

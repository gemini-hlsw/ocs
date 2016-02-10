package edu.gemini.seqexec.web.server

import edu.gemini.seqexec.web.common.Comment
import org.http4s.EntityDecoder._
import org.http4s._
import org.http4s.dsl._

import upickle.default._

import scalaz._
import Scalaz._

object RestRoutes {

  private implicit def timedES = scalaz.concurrent.Strategy.DefaultTimeoutScheduler

  val parse: String => Throwable \/ Comment =
      json => \/.fromTryCatchNonFatal { read[Comment](json) }

  implicit val commentDecoder:EntityDecoder[Comment] = EntityDecoder.decodeBy(MediaRange.`text/*`)(msg =>
        collectBinary(msg).map(bs => read[Comment](new String(bs.toArray, msg.charset.getOrElse(Charset.`UTF-8`).nioCharset))
      )
  )
  // Local in memory comments `database`
  var comments = List.empty[Comment]

  val service = HttpService {
    case req @ GET -> Root / "comments" =>
      Ok(write(comments))
    case req @ POST -> Root / "comments" =>
      req.decode[Comment]{c =>
        // There must be a nicer way to do this
        comments = comments :+ c
        Ok(write(comments))
      }
  }
}

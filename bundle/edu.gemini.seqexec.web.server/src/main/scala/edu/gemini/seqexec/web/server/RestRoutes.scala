package edu.gemini.seqexec.web.server

import edu.gemini.seqexec.web.common.Comment
import org.http4s.EntityDecoder._
import org.http4s._
import org.http4s.dsl._
import org.http4s.server.websocket.WS
import org.http4s.websocket.WebsocketBits.{Text, WebSocketFrame}

import upickle.default._

import scalaz._
import Scalaz._

import scalaz.concurrent.{Task, Strategy}
import scalaz.stream.{Exchange, Sink, Process}
import scalaz.stream.Process._
import scalaz.stream.time.awakeEvery
import scalaz.concurrent.Strategy.DefaultTimeoutScheduler

import scala.concurrent.duration._

object RestRoutes {

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
    case req@ GET -> Root / "ws" => // WebSockets end point
      val src = awakeEvery(1.seconds)(Strategy.DefaultStrategy, DefaultTimeoutScheduler).map{ d => Text(s"Ping! $d") }
      val sink: Sink[Task, WebSocketFrame] = Process.constant {
        case Text(t, _) => Task.delay( println(t))
        case f       => Task.delay(println(s"Unknown type: $f"))
      }
      WS(Exchange(src, sink))
  }
}

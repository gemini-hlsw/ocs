package edu.gemini.seqexec.web.server.play

import akka.actor.ActorSystem
import controllers.Assets
import edu.gemini.seqexec.web.common.Comment
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.routing.Router
import play.api.{BuiltInComponents, Mode}
import play.core.server.{NettyServerComponents, ServerConfig}
import play.api.routing.sird._
import play.api.mvc._

import upickle.default._

import scala.concurrent.Future
import scalaz._
import Scalaz._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.pattern.after

object WebServerLauncher extends App {
  // Local in memory comments `database`
  var comments = List.empty[Comment]

  val actorSystem = ActorSystem()

  val parse: String => Throwable \/ Comment =
      json => \/.fromTryCatchNonFatal { read[Comment](json) }

  // sends the time every second, ignores any input
  def wsTime = WebSocket.using[String] {
    request =>
      val outEnumerator: Enumerator[String] = Enumerator.generateM(after(500.milliseconds, actorSystem.scheduler)(Future.apply(Some(s"Ping at ${System.currentTimeMillis}"))))
      val inIteratee: Iteratee[String, Unit] = Iteratee.ignore[String]

      (inIteratee, outEnumerator)
  }

  def launch(port: Int):NettyServerComponents = {
    new NettyServerComponents with BuiltInComponents {
      override lazy val serverConfig = ServerConfig(
        port = Some(port),
        address = "127.0.0.1"
      ).copy(mode = Mode.Dev)

      lazy val router = Router.from {
        case GET(p"/") =>
          // Static files
          Assets.at("/", "index.html")
        case GET(p"/api/comments") => Action {
          Results.Ok(write(comments))
        }
        case GET(p"/api/ws") =>
          // Websocket route
          wsTime
        case POST(p"/api/comments") => Action { request =>
          request.body.asText.map(parse) match {
            case Some(\/-(c: Comment)) =>
              comments = comments :+ c
              Results.Ok(write(comments))
            case _                     => Results.BadRequest("Bad request")
          }
        }
        case GET(p"/$f*") =>
          // Static files
          Assets.at("/", f)
      }
  }}

  launch(9090).server

}

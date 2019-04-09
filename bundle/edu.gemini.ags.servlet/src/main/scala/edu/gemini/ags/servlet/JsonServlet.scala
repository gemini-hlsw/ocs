package edu.gemini.ags.servlet

import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.servlet.json._

import argonaut._, Argonaut._

import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }
import javax.servlet.http.HttpServletResponse.{ SC_BAD_REQUEST, SC_INTERNAL_SERVER_ERROR, SC_OK }

import scala.io.Source
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.global
import scala.util.{ Failure, Success }

import scalaz._, Scalaz._
import scalaz.concurrent.Task


final class JsonServlet(magTable: MagnitudeTable) extends HttpServlet with AgsRequestCodec with SelectionCodec {

  import JsonServlet._

  override def doPost(
    req: HttpServletRequest,
    res: HttpServletResponse
  ): Unit = {

    // Read the body, which with some luck is a JSON string
    val enc  = Option(req.getCharacterEncoding).getOrElse("UTF-8")
    val src  = Source.fromInputStream(req.getInputStream, enc)
    val json = try src.mkString finally src.close

    val task: \/[String, Task[Option[AgsStrategy.Selection]]] =
      for {
        agsReq     <- \/.fromEither(Parse.decodeEither[AgsRequest](json))
        obsContext <- agsReq.toContext
        strategy   <- AgsRegistrar.currentStrategy(obsContext) \/> "Could not determine AGS strategy."
      } yield Task.delay(strategy.select(obsContext, magTable)(global)).flatMap(fut => toTask(fut))

    val result = task.leftMap(msg => (SC_BAD_REQUEST, msg))
                     .flatMap(_.unsafePerformSyncAttempt.leftMap(e => (SC_INTERNAL_SERVER_ERROR, e.getMessage)))

    result match {
      case -\/((code, msg)) =>
        res.sendError(code, msg)

      case \/-(sel)         =>
        res.setStatus(SC_OK)
        res.setContentType("text/json; charset=UTF-8")
        val writer = res.getWriter
        println("RESULT: -------------------")
        println(sel)
        println("---")
        println(sel.asJson.spaces2)
        println("---------------------------")
        writer.write(sel.asJson.spaces2)
        writer.close
    }
  }

}

object JsonServlet {

  // Copied the idea from cats-effect `IOFromFuture`.
  private def toTask[A](f: Future[A]): Task[A] =
    f.value match {
      case Some(result) =>
        result match {
          case Success(a) => Task.now(a)
          case Failure(e) => Task.fail(e)
        }
      case _ =>
        Task.async { cb =>
          f.onComplete(r => cb(r match {
            case Success(a) => \/-(a)
            case Failure(e) => -\/(e)
          }))(global)
        }
    }

}

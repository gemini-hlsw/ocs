package edu.gemini.ags.servlet


import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.servlet.json._

import argonaut._, Argonaut._

import java.util.concurrent.{ Executors, ThreadFactory }
import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }
import javax.servlet.http.HttpServletResponse.{ SC_BAD_REQUEST, SC_INTERNAL_SERVER_ERROR, SC_OK }

import scala.io.Source
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{ Failure, Success }

import scalaz._, Scalaz._
import scalaz.concurrent.Task

/**
 * Servlet that accepts a JSON-encoded `AgsRequest` as its POST payload (no
 * other methods are supported) and responds with a JSON-encoded
 * `Option[AgsStrategy.Selection]` on success or a `SC_BAD_REQUEST` with an
 * error message on failure.  JSON codecs are defined in package
 * `edu.gemini.ags.servlet.json`.
 */
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

    // The AGS API uses `Future` but we map it to a `Task`.
    val task: \/[String, Task[Option[AgsStrategy.Selection]]] =
      for {
        agsReq     <- \/.fromEither(Parse.decodeEither[AgsRequest](json))
        obsContext <- agsReq.toContext
        strategy   <- AgsRegistrar.currentStrategy(obsContext) \/> "Could not determine AGS strategy."
      } yield Task.delay(strategy.select(obsContext, magTable)(executionContext)).flatMap(fut => toTask(fut))

    val result = task.leftMap(msg => (SC_BAD_REQUEST, msg))
                     .flatMap(_.unsafePerformSyncAttempt.leftMap(e => (SC_INTERNAL_SERVER_ERROR, e.getMessage)))

    result match {
      case -\/((code, msg)) =>
        res.sendError(code, msg)

      case \/-(sel)         =>
        res.setStatus(SC_OK)
        res.setContentType("text/json; charset=UTF-8")
        val writer = res.getWriter
        writer.write(sel.asJson.spaces2)
        writer.close
    }
  }

}

object JsonServlet {

  // Worker pool for running the queries. Puts a limit on how many concurrent
  // AGS selections can be running.  Queries block on catalog requests and then
  // do a relatively quick calculation so more threads means we hit the catalog
  // server with more simultaneous requests and throughput goes up. If we
  // increase the thread count too much, the catalog server begins failing and
  // producing HTML 500 and 504 responses.

  private val pool = Executors.newFixedThreadPool(16, new ThreadFactory() {
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r, "AGS JsonServlet - Query Worker")
      t.setPriority(Thread.NORM_PRIORITY - 1)
      t.setDaemon(true)
      t
    }
  })

  private val executionContext = ExecutionContext.fromExecutor(pool)

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
          }))(executionContext)
        }
    }

}

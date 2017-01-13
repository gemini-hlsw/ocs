package edu.gemini.spModel.io.ocs3

import edu.gemini.pot.sp.{ISPNode, SPObservationID}
import edu.gemini.pot.spdb.IDBDatabaseService

import edu.gemini.spModel.core.SPProgramID

import java.util.logging.{Level, Logger}
import java.security.Principal

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import javax.servlet.http.HttpServletResponse.{SC_BAD_REQUEST, SC_INTERNAL_SERVER_ERROR, SC_NOT_FOUND, SC_OK}

import scala.util.Try
import scalaz._
import Scalaz._


/** A program export servlet that provides program XML in a format that is
  * more easily ingested into the ocs3 model.
  */
final class Ocs3ExportServlet(db: IDBDatabaseService) extends HttpServlet {

  import Ocs3ExportServlet.Log

  case class Error(code: Int, msg: String, ex: Option[Throwable])

  object Error {
    def badRequest(path: String): Error =
      Error(SC_BAD_REQUEST, s"could not parse '$path' as a program or observation id", None)

    def notFound(id: String): Error =
      Error(SC_NOT_FOUND, s"no program or observation with id '$id'", None)

    def unexpected(id: String, ot: Option[Throwable]): Error =
      Error(SC_INTERNAL_SERVER_ERROR, s"problem exporting '$id'" + ot.map(t => s": ${t.getMessage}").orZero, ot)
  }


  type Result[A] = Error \/ A

  private implicit class ResultOps(r: Result[String]) {
    def send(res: HttpServletResponse): Unit = {
      def write(m: String): Unit = {
        val w = res.getWriter
        try { w.write(m) } finally { w.close }
      }

      r match {
        case -\/(err) =>
          Log.log(Level.WARNING, err.msg, err.ex.orNull)
          res.setStatus(err.code)
          write(err.msg)

        case \/-(xml) =>
          res.setStatus(SC_OK)
          res.setContentType("text/xml; charset=UTF-8")
          write(xml)
      }
    }
  }

  private def extractId(req: HttpServletRequest): Result[String] =
    req.getRequestURI.split('/').drop(2).toList match {
      case id :: Nil => id.right
      case x         => Error.badRequest(x.mkString("/")).left
    }

  private def fetchNode(id: String): Result[ISPNode] = {
    def lookup(n: => ISPNode): Option[ISPNode] =
      \/.fromTryCatchNonFatal(Option(n)).toOption.flatten

    val node = lookup(db.lookupObservationByID(new SPObservationID(id))) orElse
                 lookup(db.lookupProgramByID(SPProgramID.toProgramID(id)))

    node \/> Error.notFound(id)
  }

  private def fetchXml(n: ISPNode, id: String): Result[String] = {
    def unsafeRunFunctor(): Ocs3ExportFunctor =
      db.getQueryRunner(java.util.Collections.emptySet[Principal]).execute(new Ocs3ExportFunctor, n)

    \/.fromTryCatchNonFatal(unsafeRunFunctor())
      .leftMap { t => Error.unexpected(id, Some(t)) }
      .flatMap { f => f.result \/> Error.unexpected(id, None) }
  }

  override def doGet(req: HttpServletRequest, res: HttpServletResponse): Unit =
    (for {
      id <- extractId(req)
      n  <- fetchNode(id)
      x  <- fetchXml(n, id)
    } yield x).send(res)
}

object Ocs3ExportServlet {
  val Log = Logger.getLogger(this.getClass.getName)
}

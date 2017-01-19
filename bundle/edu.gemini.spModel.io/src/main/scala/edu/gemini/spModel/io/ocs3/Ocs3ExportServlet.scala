package edu.gemini.spModel.io.ocs3

import edu.gemini.pot.sp.{ISPObservationContainer, ISPNode, ISPObservation, ISPProgram, ISPTemplateGroup, SPObservationID}
import edu.gemini.pot.spdb.{IDBDatabaseService, IDBFunctor}

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.util.DBProgramListFunctor

import java.util.logging.{Level, Logger}
import java.security.Principal

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import javax.servlet.http.HttpServletResponse.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_INTERNAL_SERVER_ERROR, SC_NOT_FOUND, SC_OK}

import scala.collection.JavaConverters._

import scala.util.Try
import scalaz._
import Scalaz._

import Ocs3ExportServlet._

/** A program export servlet that provides program XML in a format that is
  * more easily ingested into the ocs3 model.
  */
final class Ocs3ExportServlet(db: IDBDatabaseService) extends HttpServlet {

  private def doCommand(cmd: Command): Result[String] =
    cmd match {
      case FetchXml(id) =>
        for {
          n  <- fetchNode(id)
          x  <- fetchXml(n, id)
        } yield x

      case ListObs(id)  =>
        fetchNode(id).map { n =>
          <obsList>
            {listObs(n).sorted.map(oid => <oid>{oid.toString}</oid>)}
          </obsList>.mkString
        }

      case ListProgs   =>
        listProgs.map { pids =>
          <progList>
            {pids.sorted.map(pid=> <pid>{pid.toString}</pid>)}
          </progList>.mkString
        }
    }

  // Fetch the ISPProgram or ISPObservation associated with the id, if any.
  private def fetchNode(id: String): Result[ISPNode] = {
    def lookup(n: => ISPNode): Option[ISPNode] =
      \/.fromTryCatchNonFatal(Option(n)).toOption.flatten

    val node = lookup(db.lookupObservationByID(new SPObservationID(id))) orElse
                 lookup(db.lookupProgramByID(SPProgramID.toProgramID(id)))

    node \/> Error.notFound(id)
  }

  // Convert the program or observation into an XML String.
  private def fetchXml(n: ISPNode, id: String): Result[String] =
    \/.fromTryCatchNonFatal {
      db.getQueryRunner(java.util.Collections.emptySet[Principal]).execute(new Ocs3ExportFunctor, n)
    }.leftMap { t => Error.exportError(id, Some(t))           }
     .flatMap { f => f.result \/> Error.exportError(id, None) }

  // List the observations contained in the given node, if any.
  private def listObs(n: ISPNode): List[SPObservationID] =
    n match {
      case p: ISPProgram     =>
        p.allObservationsIncludingTemplateObservations.map(_.getObservationID)
      case o: ISPObservation =>
        List(o.getObservationID)
      case _                 =>
        List.empty
    }

  // List the programs in the database.
  private def listProgs: Result[List[SPProgramID]] =
    \/.fromTryCatchNonFatal {
      db.getQueryRunner(java.util.Collections.emptySet[Principal]).queryPrograms(new DBProgramListFunctor)
    }.leftMap { t => Error.listProgsError(Some(t))                                                           }
     .flatMap { f => Option(f.getList).map(_.asScala.toList.map(_.programID)) \/> Error.listProgsError(None) }

  override def doGet(req: HttpServletRequest, res: HttpServletResponse): Unit =
    (for {
      _  <- checkScheme(req)
      c  <- parseRequest(req)
      x  <- doCommand(c)
    } yield x).send(res)
}

object Ocs3ExportServlet {
  val Log = Logger.getLogger(this.getClass.getName)

  case class Error(code: Int, msg: String, ex: Option[Throwable])

  object Error {
    def forbidden: Error =
      Error(SC_FORBIDDEN, s"cannot access the program or observation using this servlet", None)

    def badRequest(path: String): Error =
      Error(SC_BAD_REQUEST, s"could not parse '$path' as a program or observation id", None)

    def notFound(id: String): Error =
      Error(SC_NOT_FOUND, s"no program or observation with id '$id'", None)

    private def serverError(msg: String, ot: Option[Throwable]): Error =
      Error(SC_INTERNAL_SERVER_ERROR, msg + ot.map(t => s": ${t.getMessage}").orZero, ot)

    def listProgsError(ot: Option[Throwable]): Error =
      serverError("problem listing programs", ot)

    def exportError(id: String, ot: Option[Throwable]): Error =
      serverError(s"problem exporting '$id'", ot)
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

  // Only allow access over http, which is unavailable offsite.  This is a
  // bit of an eyebrow-raiser but we don't want to burden the client of this
  // servlet with the classes required to obtain and send keys.
  private def checkScheme(req: HttpServletRequest): Result[Unit] =
    req.getScheme match {
      case "http" => ().right
      case _      => Error.forbidden.left
    }

  sealed trait Command extends Product with Serializable

  final case class FetchXml(id: String) extends Command
  case object ListProgs                 extends Command
  final case class ListObs(pid: String) extends Command

  // Determines the command encoded in the request.
  private def parseRequest(req: HttpServletRequest): Result[Command] =
    req.getRequestURI.split('/').drop(2).toList match {
      case "fetch" :: id :: Nil => FetchXml(id).right
      case "list"  :: id :: Nil => ListObs(id).right
      case "list"  :: Nil       => ListProgs.right
      case x                    => Error.badRequest(x.mkString("/")).left
    }

}

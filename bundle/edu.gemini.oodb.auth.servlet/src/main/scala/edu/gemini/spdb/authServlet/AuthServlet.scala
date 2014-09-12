package edu.gemini.spdb.authServlet

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.security.UserRole
import edu.gemini.util.security.auth.keychain.KeyService
import edu.gemini.util.security.permission.ProgramPermission
import edu.gemini.util.security.principal.{StaffPrincipal, AffiliatePrincipal, ProgramPrincipal, GeminiPrincipal}
import edu.gemini.util.security.policy.ImplicitPolicy

import java.io.{OutputStreamWriter, BufferedWriter}
import java.util.logging.{Level, Logger}
import javax.security.auth.Subject
import javax.servlet.ServletException
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}

import scala.util.Try

import scalaz._
import Scalaz._

/**
 * This is a translation of the old-infrastructure GSA authentication servlet,
 * used by the GSA to check whether a user should have access to data for a
 * program.  Because the GSA code has not and will not be updated for the
 * 2014A release, it must match exactly the old version in terms of parameters
 * and responses.
 */

object AuthServlet {
  val Log = Logger.getLogger(this.getClass.getName)

  val IdKey       = "id"
  val PasswordKey = "password"
  val RoleKey     = "role"

  import UserRole._

  case class Params(id: SPProgramID, password: String, role: UserRole) {
    def toPrincipal: Option[GeminiPrincipal] =
      if (role == PI) Some(ProgramPrincipal(id)) // legacy support, PI role equivalent to new ProgramPrincipal
      else if (role == STAFF) Some(StaffPrincipal.Gemini)
      else Option(role.getAffiliate).map(AffiliatePrincipal)
  }

  object ParamsParser {
    def parse(req: HttpServletRequest): String \/ Params = ParamsParser(req).toParams
  }

  case class ParamsParser(req: HttpServletRequest) {
    def id: String \/ SPProgramID =
      Option(req.getParameter(IdKey)).toRightDisjunction(s"Missing parameter: $IdKey").flatMap { idStr =>
        try {
          \/-(SPProgramID.toProgramID(idStr))
        } catch {
          case _: Exception => -\/(s"Invalid program id: $idStr")
        }
      }

    def password: String \/ String =
      Option(req.getParameter(PasswordKey)).toRightDisjunction(s"Missing parameter: $PasswordKey")

    def role: String \/ UserRole = {
      val roleStr = Option(req.getParameter(RoleKey)).getOrElse(UserRole.PI.getDisplayName)
      Option(UserRole.getUserRoleByDisplayName(roleStr)).toRightDisjunction(s"Invalid role: $roleStr}")
    }

    // this stops at the first error, but so did the old servlet which i feel
    // compelled to match exactly
    def toParams: String \/ Params =
      for(i <- id; p <- password; r <- role) yield Params(i, p, r)
  }
}

import AuthServlet._

class AuthServlet(keyService: KeyService, db: IDBDatabaseService) extends HttpServlet {

  override def doGet(req: HttpServletRequest, res: HttpServletResponse): Unit = doRequest(req, res)
  override def doPost(req: HttpServletRequest, res: HttpServletResponse): Unit = doRequest(req, res)

  private def doRequest(req: HttpServletRequest, res: HttpServletResponse): Unit =
    ParamsParser.parse(req).fold(err => throw new ServletException(err), doRequest(_, res))

  private def doRequest(p: Params, res: HttpServletResponse): Unit = {

    def canRead(gp: GeminiPrincipal): Try[Unit] =
      Try(ImplicitPolicy.forJava.checkPermission(db, gp, ProgramPermission.Read(p.id)))
    
    // this is awful but matches the legacy AuthServlet response
    def send(message: String): Unit = {
      res.setContentType("text/html")
      val bw = new BufferedWriter(new OutputStreamWriter(res.getOutputStream))
      try bw.write(message) catch {
        case e: Exception =>
          Log.log(Level.WARNING, "Problem sending response", e)
          throw new ServletException(e)
      } finally bw.close()
    }

    def fail(message: String): Unit = send("NO: " + message)
    def pass(): Unit = send("YES")

    p.toPrincipal.fold(fail("permission denied")) { principal =>
      keyService.tryKey(principal, p.password).fold(
        _ => fail("bad program id and/or password"),
        _ => canRead(principal) match {
          case scala.util.Failure(_) => fail("permission denied")
          case scala.util.Success(_) => pass()
        })
    }
  }
}
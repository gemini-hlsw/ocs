package edu.gemini.sp.vcs.tui.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.sp.vcs.{OldVcsFailure, TrpcVcsServer, VersionControlSystem}
import edu.gemini.sp.vcs.log.VcsLog
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.spModel.core.{Peer, SPProgramID, SPBadIDException}

import scala.util.control.Exception._
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._
import edu.gemini.util.security.auth.keychain.KeyChain

object CommandsImpl {

  def parseId(s: String): Either[String, SPProgramID] =
    failAsValue(classOf[SPBadIDException])(Left("Sorry, '%s' isn't a valid program id.".format(s))) {
      Right(SPProgramID.toProgramID(s))
    }

  def parseLoc(s: String): Either[String, Peer] =
    Option(Peer.tryParse(s)).toRight("Sorry, expecting host:port(:site)? not '%s'".format(s))

  def catchingEverything(body: => String): String =
    try {
      body
    } catch {
      case t: java.lang.Throwable =>
        t.printStackTrace()
        "Sorry, something went wrong: %s.".format(t.getMessage)
    }

  def cmdWithIdUsage(cmd: String): String =
    "Usage: vcs %s [[host:port] program_id]".format(cmd)

  case class CmdArgs(loc: Peer, id: SPProgramID)

}

import CommandsImpl._

/**
 *
 */
class CommandsImpl(odb: IDBDatabaseService, reg: VcsRegistrar, auth: KeyChain, log: VcsLog) extends Commands {

  var defaultLocation = new Peer("localhost", 8443, null)

  var lastProgId: Option[SPProgramID] = None

  def host(): String = "default vcs host set to %s %d".format(defaultLocation.host, defaultLocation.port)

  def host(loc: Peer): String = {
    defaultLocation = loc
    host()
  }

  def locationFor(id: SPProgramID): Peer =
    reg.registration(id).getOrElse(defaultLocation)

  def parseArgs(cmd: String, args: Array[String]): Either[String, CmdArgs] =
    args.length match {
      case 2 =>
        for {
          loc <- parseLoc(args(0)).right
          id <- parseId(args(1)).right
        } yield CmdArgs(loc, id)

      case 1 => parseId(args(0)).right map {
        id => CmdArgs(locationFor(id), id)
      }
      case 0 => lastProgId.toRight(cmdWithIdUsage(cmd)).right map {
        id => CmdArgs(locationFor(id), id)
      }
      case _ => Left(cmdWithIdUsage(cmd))
    }

  def remember(args: CmdArgs) {
    lastProgId = Some(args.id)
    reg.register(args.id, args.loc)
  }

  type VcsIdOp = (SPProgramID, VersionControlSystem) => String

  def vcsIdOp(cmd: String, args: Array[String])(op: VcsIdOp): String =
    catchingEverything {
      val e = for {
        ca <- parseArgs(cmd, args).right
      } yield {
        remember(ca)
        op(ca.id, VersionControlSystem(odb, TrpcVcsServer(auth, ca.loc)))
      }
      e.merge
    }

  // TODO: have VersionControlSystem methods return an ODB perspective so we
  // TODO: know where the problem occurred?

  val checkout: VcsIdOp = (id, vcs) =>
    vcs.checkout(id).toEither.fold(
      f => OldVcsFailure.explain(f, id, "checkout", Some(locationFor(id))),
      _ => "Checked out %s".format(id.toString)
    )

  val commit: VcsIdOp = (id, vcs) =>
    vcs.commit(id).toEither.fold(
      f => OldVcsFailure.explain(f, id, "commit", Some(locationFor(id))),
      _ => "Commited %s".format(id.toString)
    )

  val status: VcsIdOp = (id, vcs) =>
    vcs.nodeStatus(id).toEither.fold(
      f => OldVcsFailure.explain(f, id, "get status", Some(locationFor(id))),
      m => StatusFormat(m)
    )

  // TODO: if already up-to-date, just say so

  val update: VcsIdOp = (id, vcs) =>
    vcs.update(id, auth.subject.getPrincipals.asScala.toSet).toEither.fold(
      f => OldVcsFailure.explain(f, id, "update", Some(locationFor(id))),
      _ => "Updated %s".format(id.toString)
    )

  val showlog: VcsIdOp = (id, vcs) =>
    vcs.log(id, 0, Int.MaxValue).toEither.fold(
      f => OldVcsFailure.explain(f, id, "log", Some(locationFor(id))),
      r => r._1.mkString("\n")
    )

  val idOps: Map[String, VcsIdOp] = Map(
    "checkout" -> checkout,
    "co" -> checkout,
    "commit" -> commit,
    "ci" -> commit,
    "status" -> status,
    "stat" -> status,
    "st" -> status,
    "update" -> update,
    "up" -> update,
    "log" -> showlog
  )

  type VcsCmdHandler = (String, Array[String]) => Option[String]

  val idCmdHandler: VcsCmdHandler = (cmd, args) =>
    idOps.get(cmd) map {
      op => vcsIdOp(cmd, args)(op)
    }

  val hostHandler: VcsCmdHandler = (cmd, args) =>
    if ("host".equals(cmd))
      args.length match {
        case 0 => Some(host())
        case 1 => Some(parseLoc(args(0)).right.map(host).merge)
        case _ => None
      }
    else None

  val migrateHandler: VcsCmdHandler = (cmd, args) =>
    Some("migrate").filter(_ == cmd).flatMap { _ =>
      args match {
        case Array(f) => Some(VcsLogMigrator.migrate(f, log).unsafePerformIO)
        case _        => None
      }
    }

  val handlers = List(idCmdHandler, hostHandler, migrateHandler)

  def vcs(cmd: String, args: Array[String]): String =
    try {
      exec(cmd, args, handlers) | "Usage: vcs host|checkout|commit|status|update|log|migrate"
    } catch {
      case e:Exception =>
        e.printStackTrace()
        "???"
    }

  def exec(cmd: String, args: Array[String], handlerList: List[VcsCmdHandler]): Option[String] =
    handlerList match {
      case h :: t => h(cmd, args) orElse exec(cmd, args, t)
      case _ => None
    }

}

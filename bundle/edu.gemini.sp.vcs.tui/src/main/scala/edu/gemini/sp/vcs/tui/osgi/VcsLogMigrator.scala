package edu.gemini.sp.vcs.tui.osgi

import edu.gemini.sp.vcs.log._
import java.io.File
import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.IO._
import edu.gemini.spModel.pio.xml.PioXmlUtil
import edu.gemini.spModel.pio._
import scala.collection.JavaConverters._
import edu.gemini.util.security.principal.{StaffPrincipal, AffiliatePrincipal, UserPrincipal}
import edu.gemini.spModel.core.{Affiliate, SPBadIDException, SPProgramID}

// One-time command for migrating old VCS log entries
object VcsLogMigrator extends App {

  // RCN: would be nice if we could derive this more easily
  type Action[+A] = EitherT[IO, String, A]
  object Action {
    def apply[A](a: String \/ A): Action[A] =
      EitherT[IO, String, A](IO(a))
  }
  implicit def ActionMonadIO = new MonadIO[Action] {
    def point[A](a: => A): Action[A] = a.point[Action]
    def bind[A, B](fa: Action[A])(f: A => Action[B]): Action[B] = fa.flatMap(f)
    def liftIO[A](ioa: IO[A]): Action[A] = EitherT[IO, String, A](ioa.map(_.right[String]))
  }

  def migrate(dirname: String, log: VcsLog): IO[String] =
    migrate0(dirname, log).run.map(_.fold(identity, identity))

  private def migrate0(dirname: String, log: VcsLog): Action[String] =
    for {
      d <- dir(dirname)
      l <- logEx(log)
      _ <- migrateDir(d, l).liftIO[Action]
    } yield "Done"

  private def dir(name: String): Action[File] =
    for {
      f <- IO(new File(name)).liftIO[Action]
      d <- IO(f.isDirectory).liftIO[Action]
      r <- Action(if (d) f.right else s"$name does not exist or is not a directory.".left)
    } yield r

  private def logEx(log: VcsLog): Action[VcsLogEx] =
    Action(log match {
      case ex: VcsLogEx => ex.right
      case _            => "The provided log doesn't have extended methods for migration.".left
    })

  private def pid(p: Container): String \/ SPProgramID =
    try {
      SPProgramID.toProgramID(p.getName).right
    } catch {
      case _: SPBadIDException => s"Not a valid program id: ${p.getName}".left
    }

  private def history(p: Container): String \/ List[ParamSet] = {
    val ps = Option(p.lookupParamSet(new PioPath("Science Program/history")))
    ps.map(_.getParamSets.asScala.toList) \/> "No history found."
  }

  case class ProgramInfo(pid: SPProgramID, history: List[ParamSet], piEmail: String) {
    println(s"ProgramInfo($pid, List(...), $piEmail)")
  }

  def container(d:Document, n: Int): String \/ Container =
    d.getContainers.asScala.lift(n) match {
      case Some(c: Container) => c.right
      case _                  => s"No container at index $n".left
    }

  def piEmail(c: Container): String \/ String =
    for {
      ps <- Option(c.lookupParamSet(new PioPath("Science Program/piInfo"))) \/> "PI Info node not found"
      pa <- Option(ps.getParam("email"))                                    \/> "Email param not found"
      em <- Option(pa.getValue)                                             \/> "Email param is empty"
    } yield if (em.isEmpty) "pi@unknown.edu" else em

  def info(d: Document): String \/ ProgramInfo =
    for {
      con <- container(d, 0)
      pid <- pid(con)
      his <- history(con)
      ema <- piEmail(con)
    } yield ProgramInfo(pid, his, ema)

  private def migrateDir(dir: File, log: VcsLogEx): IO[Unit] =
    for {
      _ <- putStrLn("Migrating fetch/store log...")
      l <- IO(dir.listFiles.filter(_.getName.toLowerCase.endsWith(".xml")).toList)
      _ <- l.traverseU(migrateFile(_, log))
    } yield ()

  private def migrateFile(xml: File, log: VcsLogEx): IO[Unit] =
    for {
      _ <- putStrLn("** " + xml.getName)
      d <- IO(PioXmlUtil.read(xml).asInstanceOf[Document])
      _ <- info(d).fold(putStrLn, h => h.history.traverseU(migrateEvent(h.pid, _, log, h.piEmail)))
    } yield ()

  private def migrateEvent(pid: SPProgramID, ps: ParamSet, log: VcsLogEx, pi: String): IO[Unit] = IO {
    val t = ps.getParam("time").getValue.toLong
    val msg = ps.getParam("message").getValue
    msg.split(" ") match {
      case Array(what, _, who) =>
        val principal = who.split("/") match {
          case Array("PI")          => UserPrincipal(pi)
          case Array("NGO", s)      => AffiliatePrincipal(Affiliate.fromString(s)) // this is unsafe :-\
          case Array("GeminiStaff") => StaffPrincipal.Gemini
        }
        val op = what match {
          case "Fetched" => OpFetch
          case "Updated" => OpStore
        }
        log.log(op, t, pid, Set(principal))
      case hmm => println("WAT?? " + msg + " ... in " + pid)
    }
  }

}

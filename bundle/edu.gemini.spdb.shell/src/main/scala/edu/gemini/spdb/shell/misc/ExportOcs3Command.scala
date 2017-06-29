package edu.gemini.spdb.shell.misc

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.io.ocs3.{ExportFormat, Ocs3ExportFunctor}
import edu.gemini.spModel.util.{DBProgramInfo, DBProgramListFunctor}

import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets.UTF_8
import java.io.{File, PrintWriter, StringWriter}
import java.security.Principal
import java.util.Set

import scala.collection.JavaConverters._

import scalaz.effect.IO
import scalaz._
import Scalaz._

import ExportOcs3Command._

/** Implements an OSGi shell command that writes OCS3-style program XML to
  * files.
  */
final class ExportOcs3Command(db: IDBDatabaseService, dir: File, user: java.util.Set[Principal]) {

  def exportOcs3(pids: java.util.List[SPProgramID]): Unit =
    exportAll(pids.asScala.toList).unsafePerformIO

  private def exportAll(pids: List[SPProgramID]): IO[Unit] = {
    def lookupPids: IO[List[SPProgramID]] =
      for {
        res <- fetchPidList.run
        ps  <- res.fold(t => IO.putStrLn(s"Could not fetch program list.\n${t.mkString}") *> IO(List.empty[SPProgramID]), IO(_))
      } yield ps

    def report(pid: SPProgramID, res: Throwable \/ File): IO[Unit] =
      IO.putStr(s"$pid: ") *> (res match {
        case -\/(t) => IO.putStrLn(s"Export failed:\n${t.mkString}")
        case \/-(f) => IO.putStrLn(s"${f.getPath}")
      })

    for {
      pids0 <- if (pids.nonEmpty) IO(pids) else lookupPids
      _     <- pids0.sorted.traverseU(pid => export(pid).run >>= (report(pid, _)))
    } yield ()
  }

  private val fetchPidList: Export[List[SPProgramID]] =
    Export(db.getQueryRunner(user).queryPrograms(new DBProgramListFunctor))
      .map { _.getList.asScala.toList.map(_.programID) }

  private def export(pid: SPProgramID): Export[File] =
    for {
      p <- Export { Option(db.lookupProgramByID(pid)).getOrElse(sys.error(s"ODB does not contain $pid")) }
      f <- export(p)
    } yield f

  private def export(p: ISPProgram): Export[File] = {
    val n = fileName(p)
    val f = new File(dir, s"$n.xml")

    for {
      fun <- Export { db.getQueryRunner(user).execute(new Ocs3ExportFunctor(ExportFormat.Ocs3), p) }
      _   <- Option(fun.getException).fold(Export.unit) { ex => Export.error(s"missing xml for $n", Some(ex)) }
      x   <- Export.fromOption(s"xml not returned by ODB for $n")(fun.result)
      _   <- Export { Files.write(f.toPath, x.getBytes(UTF_8)) }
    } yield f
  }
}

object ExportOcs3Command {


  type Export[A] = EitherT[IO, Throwable, A]

  object Export {
    def apply[A](a: => A): Export[A] =
      EitherT(IO(\/.fromTryCatchNonFatal(a)))

    def fromOption[A](msg: => String)(oa: Option[A]): Export[A] =
      EitherT.fromDisjunction(oa \/> (new RuntimeException(msg): Throwable))

    val unit: Export[Unit] =
      EitherT.right(IO.ioUnit)

    def error(msg: String, ex: Option[Throwable]): Export[Unit] =
      EitherT.left(IO(new RuntimeException(msg, ex.orNull)))
  }

  private def fileName(p: ISPProgram): String =
    Option(p.getProgramID).map(_.stringValue).getOrElse(p.getProgramKey.toString)

  implicit class ThrowableOps(t: Throwable) {
    def mkString: String = {
      val sw = new StringWriter
      val pw = new PrintWriter(sw)
      t.printStackTrace(pw)
      sw.toString
    }
  }
}

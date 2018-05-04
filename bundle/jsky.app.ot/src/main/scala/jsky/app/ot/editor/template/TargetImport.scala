package jsky.app.ot.editor.template

import edu.gemini.model.p1.immutable.Target
import edu.gemini.model.p1.targetio.api.{ DataSourceError, ParseError }
import edu.gemini.model.p1.targetio.impl.AnyTargetReader
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.shared.util.immutable.{ ImEither, ImList }
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.ProgramId
import edu.gemini.spModel.core.Semester
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.target.P1TargetConverter
import edu.gemini.spModel.target.SPTarget

import java.io.File

import javax.swing.JComponent
import javax.swing.filechooser.FileNameExtensionFilter
import scala.swing.Component
import scala.swing.FileChooser
import scala.swing.FileChooser.Result.Approve

import scalaz._
import Scalaz._
import scalaz.effect.IO

/**
 * Utility for reading a file in Phase 1 target format into a List[SPTarget].
 */
object TargetImport {

  type ImportAction[A] = EitherT[IO, String, A]

  object ImportAction {
    def apply[A](a: => A): ImportAction[A] =
      EitherT(IO(a.right))
  }

  private implicit class EitherOps[A](e: => String \/ A) {
    def liftImportAction: ImportAction[A] =
      EitherT.fromDisjunction[IO](e)
  }

  /** Extracts the site and semester from the program's id, if possible. */
  private def siteAndSemester(program: ISPProgram): String \/ (Site, Semester) =
    (for {
      p    <- Option(program)
      id   <- Option(p.getProgramID)
      pid   = ProgramId.parse(id.stringValue())
      site <- pid.site
      sem  <- pid.semester
    } yield (site, sem)) \/> "Couldn't determine the site and semester from the program id."

  /** Reads the given file into a List of P1 Target, if possible. */
  private def readFile(f: File): ImportAction[List[Target]] = {
    def format(pe: ParseError): String =
       s"${pe.targetName.map(_ + ": ").orZero}${pe.msg}"

    def summarize(lst: List[Either[ParseError, Target]]): String \/ List[Target] =
      lst.traverseU(_.validation.leftMap(_.wrapNel))
         .disjunction
         .leftMap { nel =>
           val (firstN, rest) = nel.list.toList.splitAt(5)
           firstN.map(format).mkString(
             "Problem parsing target(s):\n\n",
             "\n",
             "\n" + (if (rest.nonEmpty) "..." else "")
           )
         }

      EitherT(IO(AnyTargetReader
        .read(f)
        .disjunction
        .bimap(_.msg.left[List[Target]], summarize(_))
        .merge
      ))
  }

/** Using the site and semester, converts the list of P1 Target into SPTarget. */
  private def convertTargets(site: Site, semester: Semester, ts: List[Target]): List[SPTarget] =
    ts.map(P1TargetConverter.toSpTarget(site, _, semester.getMidpointDate(site).getTime))

  /** Reads the file and converts the contained targets into SPTarget. */
  private def readAndConvert(f: File, p: ISPProgram): ImportAction[List[SPTarget]] =
    for {
      ss <- siteAndSemester(p).liftImportAction
      ts <- readFile(f)
    } yield convertTargets(ss._1, ss._2, ts)

  /** Selects the file with a FileChooser. */
  private def selectFile(c: JComponent): ImportAction[Option[File]] = {
    val chooser = new FileChooser()
    chooser.title                 = "Select Target File"
    chooser.multiSelectionEnabled = false
    chooser.fileFilter            = new FileNameExtensionFilter("Target Files", "csv", "fits", "tst", "xml")
    ImportAction(chooser.showOpenDialog(Component.wrap(c)) match {
      case Approve => Some(chooser.selectedFile)
      case _       => None
    })
  }

  def promptAndRead(c: JComponent, p: ISPProgram): ImportAction[List[SPTarget]] =
    for {
      of <- selectFile(c)
      ts <- of.fold(ImportAction(List.empty[SPTarget]))(readAndConvert(_, p))
    } yield ts

  def promptAndReadAsJava(c: JComponent, p: ISPProgram): ImEither[String, ImList[SPTarget]] =
    promptAndRead(c, p).run.unsafePerformIO.map(_.asImList).asImEither
}

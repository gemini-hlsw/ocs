package edu.gemini.rollover.servlet

/**
 *
 */

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.spModel.core.{ProgramId, RolloverPeriod, Semester, Site}
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.rich.core._

/**
 * A predicate that determines whether a given program should be considered
 * when making the rollover report.  Programs that should be included are
 *
 * <ul>
 * <li>No older than the previous semester</li>
 * <li>Are marked active</li>
 * <li>Are not complete</li>
 * <li>Are marked as rollover candidates</li>
 * <li>Correspond to this site</li>
 * </ul>
 */
final case class IncludeProgram(site: Site, currentSemester: Semester) extends (ISPProgram => Boolean) {

  val rolloverPeriod: RolloverPeriod =
    RolloverPeriod.ending(currentSemester)

  private def progContext(progShell: ISPProgram): Option[Context] =
    for {
      rawId    <- Option(progShell.getProgramID)
      pid = ProgramId.parse(rawId.toString)
      site     <- pid.site
      semester <- pid.semester
    } yield new Context(site, semester)

  private def compatibleContext(progShell: ISPProgram): Boolean =
    progContext(progShell).exists(ctx => (ctx.site == site) && rolloverPeriod.includes(ctx.semester))

  private def isActiveRollover(progShell: ISPProgram): Boolean = {
    val sp = progShell.getDataObject.asInstanceOf[SPProgram]
    sp.isActive && !sp.isCompleted  && sp.getRolloverStatus
  }

  def apply(progShell: ISPProgram): Boolean =
    isActiveRollover(progShell) && compatibleContext(progShell)
}

object IncludeProgram {
  def apply(site: Site): IncludeProgram = IncludeProgram(site, new Semester(site))
}

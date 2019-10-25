package edu.gemini.spModel.util

import java.util.Date

import edu.gemini.pot.sp.ISPNode
import edu.gemini.spModel.core.{Site, Semester, SPProgramID, ProgramId}
import edu.gemini.spModel.obs.SchedulingBlock

import scalaz._, Scalaz._

// N.B. this causes the object methods to appear as static methods on DefaultSchedulingBlock
// which makes it easier to call from Java.
sealed abstract class DefaultSchedulingBlock
object DefaultSchedulingBlock {

  /**
   * Construct a default scheduling block for program in which `node` lives. This block will
   * start at the center of the program semester OR the current semester, whichever is later,
   * and will have no duration.
   */
  def forProgram(node: ISPNode): SchedulingBlock =
    getSchedulingBlock(node, Site.GS)

  def forPid(pid: SPProgramID): SchedulingBlock =
    SchedulingBlock(
      getPidSemesterCenter(pid).foldLeft(getCurrentSemesterCenter(Site.GS))(_ max _)
    )

  private def getSchedulingBlock(node: ISPNode, defaultSite: Site): SchedulingBlock =
    SchedulingBlock(getSchedulingBlockStart(node, defaultSite))

  private def getSchedulingBlockStart(node: ISPNode, defaultSite: Site): Long =
    getProgramSemesterCenter(node).foldLeft(getCurrentSemesterCenter(defaultSite))(_ max _)

  private def getProgramSemesterCenter(node: ISPNode): Option[Long] =
    Option(node.getProgramID).flatMap(getPidSemesterCenter)

  private def getPidSemesterCenter(spid: SPProgramID): Option[Long] =
    for {
      pid  <- ProgramId.parseStandardId(spid.stringValue)
      sem  <- pid.semester
      site <- pid.site
    } yield center(sem, site)

  private def getCurrentSemesterCenter(site: Site): Long =
    center(new Semester(site, System.currentTimeMillis), site)

  private def center(sem: Semester, site: Site): Long =
    center(sem.getStartDate(site), sem.getEndDate(site))

  private def center(a: Date, b: Date): Long =
    center(a.getTime, b.getTime)

  private def center(a: Long, b: Long): Long =
    a + (b - a) / 2

}
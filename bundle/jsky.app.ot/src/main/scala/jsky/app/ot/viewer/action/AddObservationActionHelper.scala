package jsky.app.ot.viewer.action

import java.util.Date

import edu.gemini.pot.sp.ISPNode
import edu.gemini.spModel.core.{Site, Semester, ProgramId}
import edu.gemini.spModel.obs.SchedulingBlock

import scalaz._, Scalaz._

sealed abstract class AddObservationActionHelper
object AddObservationActionHelper {

  def getSchedulingBlock(node: ISPNode): SchedulingBlock =
    getSchedulingBlock(node, Site.GS)

  def getSchedulingBlock(node: ISPNode, defaultSite: Site): SchedulingBlock =
    SchedulingBlock(getSchedulingBlockStart(node, defaultSite))

  def getSchedulingBlockStart(node: ISPNode, defaultSite: Site): Long =
    getProgramSemesterCenter(node).foldLeft(getCurrentSemesterCenter(defaultSite))(_ max _)

  def getProgramSemesterCenter(node: ISPNode): Option[Long] =
    for {
      spid <- Option(node.getProgramID)
      pid  <- ProgramId.parseStandardId(spid.stringValue)
      sem  <- pid.semester
      site <- pid.site
    } yield center(sem, site)

  def getCurrentSemesterCenter(site: Site): Long =
    center(new Semester(site, System.currentTimeMillis), site)

  def center(sem: Semester, site: Site): Long =
    center(sem.getStartDate(site), sem.getEndDate(site))

  def center(a: Date, b: Date): Long =
    center(a.getTime, b.getTime)

  def center(a: Long, b: Long): Long =
    a + (b - a) / 2

}
package edu.gemini.qv.plugin.util

import edu.gemini.skycalc.ImprovedSkyCalc
import edu.gemini.spModel.core.Site
import java.util.{Date, TimeZone}

/**
 * An artificial time zone to represent local sidereal time for a site.
 */
class LstTimeZone(site: Site) extends TimeZone {
  private val skyCalc = new ImprovedSkyCalc(site)

  def setRawOffset(p1: Int) = {}

  def getOffset(era: Int, year: Int, month: Int, day: Int, dayOfWeek: Int, milliseconds: Int): Int = 0

  override def getOffset(t: Long) = (skyCalc.getLst(new Date(t)).getTime - t).toInt

  def getRawOffset: Int = 0

  def useDaylightTime(): Boolean = false

  def inDaylightTime(p1: Date): Boolean = false
}

/**
 * Local sidereal time zones for our sites.
 */
object LstTimeZone {
  private val lstSouth = new LstTimeZone(Site.GS)
  private val lstNorth = new LstTimeZone(Site.GN)
  def apply(site: Site) = site match {
    case Site.GN => lstNorth
    case Site.GS => lstSouth
  }
}

package edu.gemini.dataman.core

import edu.gemini.spModel.core.Site

import scalaz._

/** Data Manager configuration values, extracted from bundle context.
  */
final case class DmanConfig(
                   archiveHost: GsaHost.Archive,
                   summitHost: GsaHost.Summit,
                   gsaAuth: GsaAuth,
                   site: Site,
                   tonightPeriod: PollPeriod.Tonight,
                   thisWeekPeriod: PollPeriod.ThisWeek,
                   allPeriod: PollPeriod.AllPrograms) {

  def show: String =
    s"""
      |Data Manager Config
      | Archive Host = ${archiveHost.host}
      | Summit Host  = ${summitHost.host}
      | GSA Auth     = ${gsaAuth.value}
      | Site         = ${site.abbreviation}
      | Poll Periods ------------------------
      | Tonight      = ${tonightPeriod.time}
      | This Week    = ${thisWeekPeriod.time}
      | All Programs = ${allPeriod.time}
     """.stripMargin
}

object DmanConfig {
  implicit val EqualDmanConfig: Equal[DmanConfig] = Equal.equalA
}
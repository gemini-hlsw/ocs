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
                   obsRefreshPeriod: PollPeriod.ObsRefresh,
                   archivePoll: PollPeriod.Archive,
                   summitPoll: PollPeriod.Summit) {

  def show: String =
    s"""
      |Data Manager Config
      | Archive Host = ${archiveHost.host}
      | Summit Host  = ${summitHost.host}
      | GSA Auth     = ${gsaAuth.value}
      | Site         = ${site.abbreviation}
      | Obs Refresh  = ${obsRefreshPeriod.time}
      | Archive Poll Periods ------------------------
      | Tonight      = ${archivePoll.tonight.time}
      | This Week    = ${archivePoll.thisWeek.time}
      | All Programs = ${archivePoll.allPrograms.time}
      | Summit Poll Periods -------------------------
      | Tonight      = ${summitPoll.tonight.time}
      | This Week    = ${summitPoll.thisWeek.time}
      | All Programs = ${summitPoll.allPrograms.time}
     """.stripMargin
}

object DmanConfig {
  implicit val EqualDmanConfig: Equal[DmanConfig] = Equal.equalA
}
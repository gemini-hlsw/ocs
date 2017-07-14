package edu.gemini.model.p1

import immutable.{SpecialProposalType, ExchangePartner, NgoPartner}

package object submit {

  import SubmitDestination._

  val phase1ServerUrl = "https://phase1.gemini.edu/cgi-bin/backend"

  def submissionUrls(prefix: String): Map[SubmitDestination, String] = {
    def url(s:String) = s"$prefix/${s.toLowerCase}/xmlbackend.cgi"

    val specialProposalsTestUrls = for {
        st      <- SpecialProposalType.values.toSeq
      } yield Special(st) -> url(st.name)

    NgoPartner.values.map(p => Ngo(p) -> url(p.name)).toMap ++
    ExchangePartner.values.map(p => Exchange(p) -> url(p.name)).toMap ++
    specialProposalsTestUrls ++
    Map(LargeProgram -> url("large_program"), FastTurnaroundProgram -> url("fast_turnaround"), SubaruIntensiveProgram -> url("subaru_intensive_program"))
  }

  val productionSubmissionUrls: Map[SubmitDestination, String] = submissionUrls(s"$phase1ServerUrl/production")

  val testSubmissionUrls: Map[SubmitDestination, String] = submissionUrls(s"$phase1ServerUrl/test")

}

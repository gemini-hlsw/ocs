package edu.gemini.model.p1

import immutable.{SpecialProposalType, ExchangePartner, NgoPartner}

package object submit {

  import SubmitDestination._

  def submissionUrls(prefix: String): Map[SubmitDestination, String] = {
    def url(s:String) = s"$prefix/${s.toLowerCase}/xmlbackend.cgi"

    val specialProposalsTestUrls = for {
        st      <- SpecialProposalType.values.toSeq
      } yield Special(st) -> url(st.name)

    NgoPartner.values.map(p => Ngo(p) -> url(p.name)).toMap ++
    ExchangePartner.values.map(p => Exchange(p) -> url(p.name)).toMap ++
    specialProposalsTestUrls ++
    Map(LargeProgram -> url("large_program"), FastTurnaroundProgram -> url("fast_turnaround"))
  }

  val productionSubmissionUrls: Map[SubmitDestination, String] = submissionUrls("http://phase1.cl.gemini.edu/cgi-bin/backend/production")

  val testSubmissionUrls: Map[SubmitDestination, String] = submissionUrls("http://phase1.cl.gemini.edu/cgi-bin/backend/test")

}

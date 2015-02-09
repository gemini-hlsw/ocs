package edu.gemini.model.p1

import edu.gemini.model.p1.immutable.ExchangePartner._
import edu.gemini.model.p1.immutable.NgoPartner._
import edu.gemini.model.p1.immutable.SpecialProposalType._
import edu.gemini.model.p1.submit.SubmitDestination
import mutable.{SpecialProposalType, ExchangePartner, NgoPartner}

package object submit {

  import SubmitDestination._

  val productionSubmissionUrls: Map[SubmitDestination, String] = Map(
    Ngo(AR)                      -> "http://168.83.13.30:9999/cgi-bin/gemini/xmlbackend.cgi",
    Ngo(AU)                      -> "http://www.aao.gov.au/cgi-bin/geminixml.cgi",
    Ngo(BR)                      -> "http://www.lna.br/cgi-bin/gemini/xmlbackend.cgi",
    Ngo(CA)                      -> "http://gemini.astrosci.ca:80/cgi-bin/xmlbackend.cgi",
    Ngo(CL)                      -> "http://geminicyt.conicyt.cl/cgi-bin/gemini/xmlbackend.cgi",
    Ngo(KR)                      -> "http://phase1.gemini.edu/cgi-bin/gemini/kr/xmlbackend.cgi",
    Ngo(US)                      -> "http://www.noao.edu/cgi-bin/gemini/xmlbackend.cgi",
    Ngo(UH)                      -> "http://www.ifa.hawaii.edu/cgi-bin/gemini/xmlbackend.cgi",
    Exchange(KECK)               -> "http://phase1.gemini.edu/cgi-bin/gemini/keck/xmlbackend.cgi",
    Exchange(SUBARU)             -> "http://phase1.gemini.edu/cgi-bin/gemini/subaru/xmlbackend.cgi",
    Exchange(CFHT)               -> "http://phase1.gemini.edu/cgi-bin/gemini/cfht/xmlbackend.cgi",
    Special(DEMO_SCIENCE)        -> "http://phase1.gemini.edu/cgi-bin/gemini/demo_science/xmlbackend.cgi",
    Special(POOR_WEATHER)        -> "http://phase1.gemini.edu/cgi-bin/gemini/poor_weather/xmlbackend.cgi",
    Special(SYSTEM_VERIFICATION) -> "http://phase1.gemini.edu/cgi-bin/gemini/system_verification/xmlbackend.cgi",
    Special(DIRECTORS_TIME)      -> "http://phase1.gemini.edu/cgi-bin/gemini/directors_time/xmlbackend.cgi",
    LargeProgram                 -> "http://phase1.gemini.edu/cgi-bin/gemini/large_program/xmlbackend.cgi",
    FastTurnaroundProgram        -> "http://phase1.gemini.edu/cgi-bin/gemini/fast_turnaround/xmlbackend.cgi"
  )

  val testSubmissionUrls: Map[SubmitDestination, String] = {
    def url(s:String) = s"http://phase1.cl.gemini.edu/cgi-bin/gemini/test/${s.toLowerCase}/xmlbackend.cgi"

    // REL-1257 It was requested to support simultaneous SV and non SV backends
    val specialProposalsTestUrls = for {
      st      <- SpecialProposalType.values.toSeq
    } yield if (st == SpecialProposalType.DIRECTORS_TIME || st == SpecialProposalType.POOR_WEATHER) {
        Special(st) -> url(s"${st.name}_sv")
      }  else {
        Special(st) -> url(st.name)
      }

    NgoPartner.values.map(p => Ngo(p) -> url(p.name)).toMap ++
    ExchangePartner.values.map(p => Exchange(p) -> url(p.name)).toMap ++
    specialProposalsTestUrls ++
    Map(LargeProgram -> url("large_program"), FastTurnaroundProgram -> url("fast_turnaround"))
  }

}

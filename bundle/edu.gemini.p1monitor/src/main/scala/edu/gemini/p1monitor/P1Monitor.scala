package edu.gemini.p1monitor

import edu.gemini.model.p1.immutable.SpecialProposalType

/**
 * Holds common utilities and constants
 */
object P1Monitor {
  // Implicit conversions
  val string2ProposalType = (proposalType:String) => proposalType match {
    case "DS" => SpecialProposalType.DEMO_SCIENCE
    case "DT" => SpecialProposalType.DIRECTORS_TIME
    case "PW" => SpecialProposalType.POOR_WEATHER
    case "SV" => SpecialProposalType.SYSTEM_VERIFICATION
    case "GT" => SpecialProposalType.GUARANTEED_TIME
  }
  implicit val proposalType2String = (proposalType:SpecialProposalType) => proposalType match {
    case SpecialProposalType.DEMO_SCIENCE        => "DS"
    case SpecialProposalType.DIRECTORS_TIME      => "DT"
    case SpecialProposalType.POOR_WEATHER        => "PW"
    case SpecialProposalType.SYSTEM_VERIFICATION => "SV"
    case SpecialProposalType.GUARANTEED_TIME     => "GT"
    case x => sys.error("unsupported proposal type " + x)
  }
}

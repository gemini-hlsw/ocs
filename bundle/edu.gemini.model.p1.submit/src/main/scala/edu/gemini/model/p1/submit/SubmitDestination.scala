package edu.gemini.model.p1.submit

import edu.gemini.model.p1.immutable.{Partners, ExchangePartner, NgoPartner, SpecialProposalType}

sealed trait SubmitDestination {
  def destinationName: String
}

object SubmitDestination {
  case class Ngo(p: NgoPartner) extends SubmitDestination {
    override val destinationName = Partners.name.getOrElse(p, "Unknown")
  }
  case class Exchange(p: ExchangePartner) extends SubmitDestination {
    override val destinationName = Partners.name.getOrElse(p, "Unknown")
  }
  case class Special(p: SpecialProposalType) extends SubmitDestination {
    override val destinationName = p.value
  }
  case object LargeProgram extends SubmitDestination {
    override val destinationName = "Large Program"
  }
  case object FastTurnaroundProgram extends SubmitDestination {
    override val destinationName = "Fast Turnaround"
  }
  case object SubaruIntensiveProgram extends SubmitDestination {
    override val destinationName = "Subaru Intensive"
  }
}


package edu.gemini.model.p1.submit

import edu.gemini.model.p1.immutable._
import java.util.UUID
import edu.gemini.model.p1.submit.SubmitDestination._
import scalaz._
import Scalaz._
import edu.gemini.model.p1.submit.SubmitDestination.Exchange
import edu.gemini.model.p1.submit.SubmitDestination.Special
import edu.gemini.model.p1.submit.SubmitResult.Success
import edu.gemini.model.p1.submit.SubmitDestination.Ngo

sealed trait SubContainer {
  def pc: ProposalClass
  def reset: SubContainer
  def withKey: SubContainer = pc.key ? this | withNewKey
  def withNewKey: SubContainer
  def pendingDestinations: List[SubmitDestination]
  def +(res: DestinationSubmitResult): SubContainer

  def update(proposal: Proposal): Proposal = Proposal.proposalClass.set(proposal, pc)
}

object SubContainer {

  def apply(p: Proposal): SubContainer = apply(p.proposalClass)

  def apply(pc: ProposalClass): SubContainer = pc match {
    case q: QueueProposalClass          => GeminiSubContainer(q)
    case c: ClassicalProposalClass      => GeminiSubContainer(c)
    case s: SpecialProposalClass        => SpecialSubContainer(s)
    case e: ExchangeProposalClass       => ExchangeSubContainer(e)
    case l: LargeProgramClass           => LPSubContainer(l)
    case i: SubaruIntensiveProgramClass => SIPSubContainer(i)
    case f: FastTurnaroundProgramClass  => FTSubContainer(f)
  }

  // this has side effects unfortunately
  def newKey = Some(UUID.randomUUID)

  def toResponse(suc: Success): SubmissionResponse =
    SubmissionResponse(SubmissionReceipt(suc.partnerRef, suc.timestamp, Option(suc.contact)))

  def toResponse(res: DestinationSubmitResult): Option[SubmissionResponse] =
    res.result match {
      case s: Success => Some(toResponse(s))
      case _          => None
    }

  case class GeminiSubContainer(pc: GeminiNormalProposalClass) extends SubContainer {
    private def resetSubs: Either[List[NgoSubmission], ExchangeSubmission] =
      pc.subs.left.map(_.map(_.reset)).right.map(_.reset)

    def reset = GeminiSubContainer(pc match {
      case q: QueueProposalClass     => q.copy(key = None, subs = resetSubs)
      case c: ClassicalProposalClass => c.copy(key = None, subs = resetSubs)
    })

    def withNewKey = GeminiSubContainer(pc match {
      case q: QueueProposalClass     => q.copy(key = newKey)
      case c: ClassicalProposalClass => c.copy(key = newKey)
    })

    def pendingDestinations: List[SubmitDestination] = {
      val ngos = pc.subs.left.map(_.filter(_.response.isEmpty).map(sub => Ngo(sub.partner))).left.getOrElse(Nil)
      val exc  = pc.subs.right.map(
        sub => if (sub.response.isEmpty) List(Exchange(sub.partner)) else Nil
      ).right.getOrElse(Nil)
      ngos ++ exc
    }

    private def updatedSubs(res: DestinationSubmitResult): Either[List[NgoSubmission], ExchangeSubmission] =
      res.destination match {
        case Ngo(p)      =>
          pc.subs.left.map { _.map { sub =>
            if (sub.partner == p) sub.copy(response = toResponse(res)) else sub
          }}
        case Exchange(p) =>
          pc.subs.right.map { sub =>
            if (sub.partner == p) sub.copy(response = toResponse(res)) else sub
          }
        case _ => pc.subs
      }

    def +(res: DestinationSubmitResult) = GeminiSubContainer(pc match {
      case q: QueueProposalClass     => q.copy(subs = updatedSubs(res))
      case c: ClassicalProposalClass => c.copy(subs = updatedSubs(res))
    })
  }

  object SpecialSubContainer {
    lazy val subResponse: Lens[SpecialProposalClass, Option[SubmissionResponse]] = SpecialProposalClass.sub andThen SpecialSubmission.response
  }

  case class SpecialSubContainer(pc: SpecialProposalClass) extends SubContainer {
    def reset      = SpecialSubContainer(pc.copy(key = None, sub = pc.sub.reset))
    def withNewKey = SpecialSubContainer(pc.copy(key = newKey))

    def pendingDestinations: List[SubmitDestination] =
      if (pc.sub.response.isEmpty) List(Special(pc.sub.specialType)) else Nil

    import SpecialSubContainer.subResponse

    def +(res: DestinationSubmitResult): SubContainer =
      res.destination match {
        case Special(t) if t == pc.sub.specialType =>
          SpecialSubContainer(subResponse.set(pc, toResponse(res)))
        case _ => this
      }
  }

  object LPSubContainer {
    lazy val subResponse: Lens[LargeProgramClass, Option[SubmissionResponse]] = LargeProgramClass.sub andThen LargeProgramSubmission.response
  }

  case class LPSubContainer(pc: LargeProgramClass) extends SubContainer {
    def reset      = LPSubContainer(pc.copy(key = None, sub = pc.sub.reset))
    def withNewKey = LPSubContainer(pc.copy(key = newKey))

    def pendingDestinations: List[SubmitDestination] =
      if (pc.sub.response.isEmpty) List(LargeProgram) else Nil

    import LPSubContainer.subResponse

    def +(res: DestinationSubmitResult): SubContainer =
      res.destination match {
        case LargeProgram =>
          LPSubContainer(subResponse.set(pc, toResponse(res)))
        case _            => this
      }
  }

  object SIPSubContainer {
    lazy val subResponse: Lens[SubaruIntensiveProgramClass, Option[SubmissionResponse]] = SubaruIntensiveProgramClass.sub andThen SubaruIntensiveProgramSubmission.response
  }

  case class SIPSubContainer(pc: SubaruIntensiveProgramClass) extends SubContainer {
    def reset      = SIPSubContainer(pc.copy(key = None, sub = pc.sub.reset))
    def withNewKey = SIPSubContainer(pc.copy(key = newKey))

    def pendingDestinations: List[SubmitDestination] =
      if (pc.sub.response.isEmpty) List(SubaruIntensiveProgram) else Nil

    import SIPSubContainer.subResponse

    def +(res: DestinationSubmitResult): SubContainer =
      res.destination match {
        case SubaruIntensiveProgram =>
          SIPSubContainer(subResponse.set(pc, toResponse(res)))
        case _                      => this
      }
  }

  object FTSubContainer {
    lazy val subResponse: Lens[FastTurnaroundProgramClass, Option[SubmissionResponse]] = FastTurnaroundProgramClass.sub andThen FastTurnaroundSubmission.response
  }

  case class FTSubContainer(pc: FastTurnaroundProgramClass) extends SubContainer {
    def reset      = FTSubContainer(pc.copy(key = None, sub = pc.sub.reset))
    def withNewKey = FTSubContainer(pc.copy(key = newKey))

    def pendingDestinations: List[SubmitDestination] =
      if (pc.sub.response.isEmpty) List(FastTurnaroundProgram) else Nil

    import FTSubContainer.subResponse

    def +(res: DestinationSubmitResult): SubContainer =
      res.destination match {
        case FastTurnaroundProgram =>
          FTSubContainer(subResponse.set(pc, toResponse(res)))
        case _ => this
      }
  }

  case class ExchangeSubContainer(pc: ExchangeProposalClass) extends SubContainer {
    def reset      = ExchangeSubContainer(pc.copy(key = None, subs = pc.subs.map(_.reset)))
    def withNewKey = ExchangeSubContainer(pc.copy(key = newKey))

    def pendingDestinations: List[SubmitDestination] =
      pc.subs.filter(_.response.isEmpty).map(sub => Ngo(sub.partner))

    def +(res: DestinationSubmitResult): SubContainer =
      res.destination match {
        case Ngo(p) =>
          ExchangeSubContainer(pc.copy(subs = pc.subs.map { sub =>
            if (sub.partner == p) sub.copy(response = toResponse(res)) else sub
          }))
        case _ => this
      }

  }
}
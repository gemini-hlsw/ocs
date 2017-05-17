package edu.gemini.phase2.skeleton.factory

import edu.gemini.spModel.core.Affiliate
import edu.gemini.spModel.gemini.obscomp.SPProgram.ProgramMode._
import edu.gemini.spModel.too.TooType
import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.immutable.ExchangePartner._
import edu.gemini.model.p1.immutable.NgoPartner._
import edu.gemini.model.p1.immutable.SpecialProposalType._
import edu.gemini.model.p1.mutable.InvestigatorStatus.GRAD_THESIS
import edu.gemini.model.p1.mutable.TooOption
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.gemini.obscomp.SPProgram.{PIInfo, ProgramMode}
import edu.gemini.shared.util.TimeValue

import edu.gemini.spModel.timeacct.{TimeAcctAllocation, TimeAcctAward, TimeAcctCategory}
import edu.gemini.spModel.gemini.phase1.{GsaPhase1Data => Gsa}

import java.time.Duration

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/**
 * Factory for creating an SPProgram from a Phase 1 Proposal document.
 */
object SpProgramFactory {

  private val MsPerHour = Duration.ofHours(1).toMillis

  private val NGO_TIME_ACCT = Map(
    AR -> TimeAcctCategory.AR,
    AU -> TimeAcctCategory.AU,
    BR -> TimeAcctCategory.BR,
    CA -> TimeAcctCategory.CA,
    CL -> TimeAcctCategory.CL,
    KR -> TimeAcctCategory.KR,
    UH -> TimeAcctCategory.UH,
    US -> TimeAcctCategory.US
  )

  private val EXC_TIME_ACCT = Map(
    CFHT   -> TimeAcctCategory.CFH,
    SUBARU -> TimeAcctCategory.JP,
    KECK   -> TimeAcctCategory.XCHK
  )

  private val SPC_TIME_ACCT = Map(
    DEMO_SCIENCE        -> TimeAcctCategory.DS,
    DIRECTORS_TIME      -> TimeAcctCategory.DD,
    SYSTEM_VERIFICATION -> TimeAcctCategory.SV
    // NOTE: no category for poor weather
  )

  def create(proposal: Proposal): SPProgram = {
    val prog = new SPProgram()
    prog.setTitle(proposal.title)

    val pmode = mode(proposal)
    prog.setProgramMode(pmode)
    if (pmode == QUEUE) {
      // TODO: use TooTypeSetter
      prog.setTooType(too(proposal))
      // REL-2079 Set default to 1
      val b = band(proposal).getOrElse(1)
      prog.setQueueBand(b.toString)
    }
    prog.setRolloverStatus(isRollover(proposal))
    prog.setThesis(isThesis(proposal))

    prog.setPIInfo(piInfo(proposal))
    hostNgoEmail(proposal) foreach { e => prog.setPrimaryContactEmail(e) }

    // Note: not a typo -- "contact person" is an email
    gemEmail(proposal) foreach { e => prog.setContactPerson(e) }

    minBand3Time(proposal) foreach { tv => prog.setMinimumTime(tv) }
    timeAcctAllocation(proposal) foreach { alloc => prog.setTimeAcctAllocation(alloc) }

    prog.setGsaPhase1Data(gsaPhase1Data(proposal))
    prog
  }

  private def piInfo(proposal: Proposal): PIInfo = {
    val pi    = proposal.investigators.pi
    val first = pi.firstName
    val last  = pi.lastName
    val email = pi.email
    val phone = pi.phone.mkString(",")
    val aff   = affiliate(proposal)
    new PIInfo(first, last, email, phone, aff.orNull)
  }


  def mode(proposal: Proposal): ProgramMode =
    proposal.proposalClass match {
      case c: ClassicalProposalClass => CLASSICAL
      case _                         => QUEUE
    }

  def band(proposal: Proposal): Option[Int] =
    for {
      i <- proposal.proposalClass.itac
      b <- band(i)
    } yield b

  private def band(itac: Itac): Option[Int] = itac.decision.right.map(_.band).right.toOption

  def too(proposal: Proposal): TooType =
    proposal.proposalClass match {
      case q: QueueProposalClass => too(q.tooOption)
      case _                     => TooType.none
    }

  private def too(tooType: TooOption): TooType =
    tooType match {
      case TooOption.STANDARD => TooType.standard
      case TooOption.RAPID    => TooType.rapid
      case _                  => TooType.none
    }

  /**
   * The host submission is the proposal's NgoSubmission (if any) that was
   * accepted and had the largest time award.
   */
  def hostSubmission(proposal: Proposal): Option[NgoSubmission] =
    proposal.proposalClass match {
      case n: GeminiNormalProposalClass => hostSubmission(n)
      case e: ExchangeProposalClass     => hostSubmission(e.subs)
      case _                            => None
    }

  private def hostSubmission(n: GeminiNormalProposalClass): Option[NgoSubmission] =
    for {
      l <- n.subs.left.toOption
      h <- hostSubmission(l)
    } yield h

  private def hostSubmission(l: List[NgoSubmission]): Option[NgoSubmission] =
    if (l.isEmpty) {
      None
    } else {
      Some(l.maxBy(s => timeAward(s).getOrElse(TimeAmount.empty).hours))
    }

  private def acceptance(sub: Submission): Option[SubmissionAccept] =
    for {
      r <- sub.response
      d <- r.decision
      a <- d.decision.right.toOption
    } yield a

  private def timeAward(ngo: NgoSubmission): Option[TimeAmount] =
    acceptance(ngo) map { a => a.recommended }

  def affiliate(proposal: Proposal): Option[Affiliate] =
    proposal.proposalClass match {
      case _: LargeProgramClass => Some(Affiliate.GEMINI_STAFF)
      case _                    => hostSubmission(proposal) flatMap { s => affiliate(s.partner) }
    }

  def hostNgoEmail(p: Proposal): Option[String] =
    for {
      h <- hostSubmission(p)
      a <- acceptance(h)
    } yield a.email

  private def affiliate(ngoPartner: NgoPartner): Option[Affiliate] =
    Option(Affiliate.fromString(ngoPartner.value()))

  private def itacAcceptance(proposal: Proposal): Option[ItacAccept] =
    for {
      i <- proposal.proposalClass.itac
      a <- i.decision.right.toOption
    } yield a

  def isRollover(proposal: Proposal): Boolean =
    itacAcceptance(proposal).exists(_.rollover)

  def isThesis(proposal: Proposal): Boolean =
    proposal.investigators.all exists { i => i.status == GRAD_THESIS }

  def gemEmail(proposal: Proposal): Option[String] =
    itacAcceptance(proposal).flatMap(_.contact)

  def minBand3Time(proposal: Proposal): Option[TimeValue] =
    proposal.proposalClass match {
      case q: QueueProposalClass => q.band3request map { r => toTimeValue(r.minTime) }
      case _                     => None
    }

  private def toTimeValue(ta: TimeAmount): TimeValue =
    ta.units match {
      case TimeUnit.NIGHT => new TimeValue(ta.value, TimeValue.Units.nights)
      case _              => new TimeValue(ta.toHours.value, TimeValue.Units.hours)
    }

  def timeAcctAllocation(proposal: Proposal): Option[TimeAcctAllocation] =
    awardedHours(proposal).filter(_ > 0.0) flatMap { hrs =>
      val progTime = hoursFromObservations(proposal, _.progTime)
      val partTime = hoursFromObservations(proposal, _.partTime)

      timeAccountingRatios(proposal) match {
        case Nil => None
        case ratios =>
          val jmap = ratios.map { case (cat, rat) =>
            def durationRatio(v: TimeAmount): Duration =
              Duration.ofMillis(((v.hours * rat) * MsPerHour).round)

            val award = new TimeAcctAward(durationRatio(progTime), durationRatio(partTime))
            (cat, award)
          }.toMap.asJava
          Some(new TimeAcctAllocation(jmap))
      }
    }

  def awardedHours(proposal: Proposal): Option[Double] =
    itacAcceptance(proposal) map { a => a.award.toHours.value }

  private def hoursFromObservations(proposal: Proposal, sf: Observation => Option[TimeAmount]): TimeAmount =
    proposal.observations.foldLeft(TimeAmount.empty)(_ |+| sf(_).getOrElse(TimeAmount.empty))

  def timeAccountingRatios(proposal: Proposal): List[(TimeAcctCategory, Double)] =
    proposal.proposalClass match {
      case n: GeminiNormalProposalClass  =>
        n.subs match {
          case Left(ngos) => ngoRatios(ngos)
          case Right(exc) => excRatio(exc).toList
        }
      case _: LargeProgramClass          => List((TimeAcctCategory.LP, 1.0))
      case e: ExchangeProposalClass      => ngoRatios(e.subs)
        // TBD This part of the code won't be triggered because FT doesn't go through itac (See awardedHours function)
        // But we'll leave it here for the future
      case f: FastTurnaroundProgramClass =>
        // In principle there are no proposals submitted without a PA but check just in case
        ~f.partnerAffiliation.collect {
          case -\/(ngo) =>
            val s = NgoSubmission(f.sub.request, f.sub.response, ngo, InvestigatorRef(proposal.investigators.pi))
            ngoRatios(List(s))
          case \/-(exc) =>
            val s = ExchangeSubmission(f.sub.request, f.sub.response, exc, InvestigatorRef(proposal.investigators.pi))
            excRatio(s).toList
        }
      case s: SpecialProposalClass       => spcRatio(s.sub).toList
    }

  private def ngoRatios(subs: List[NgoSubmission]): List[(TimeAcctCategory, Double)] = {
    val catHrs = categorizedHours(subs)
    val total  = catHrs.unzip._2.sum

    if (total == 0.0) {
      catHrs map { case (cat, _) => (cat, 1.0 / catHrs.length)}
    } else {
      catHrs map { case (cat, hrs) => (cat, hrs / total)}
    }
  }

  private def categorizedHours(subs: List[NgoSubmission]): List[(TimeAcctCategory, Double)] =
    for {
      ngo <- subs
      cat <- NGO_TIME_ACCT.get(ngo.partner)
      acc <- acceptance(ngo) if acc.recommended.hours >= 0.0
    } yield (cat, acc.recommended.hours)

  private def excRatio(exc: ExchangeSubmission): Option[(TimeAcctCategory, Double)] =
    EXC_TIME_ACCT.get(exc.partner) map { cat => (cat, 1.0) }

  private def spcRatio(spc: SpecialSubmission): Option[(TimeAcctCategory, Double)] =
    SPC_TIME_ACCT.get(spc.specialType) map { cat => (cat, 1.0) }

  def gsaPhase1Data(proposal: Proposal): Gsa = {
    val abstrakt = new Gsa.Abstract(proposal.abstrakt)
    val category = new Gsa.Category(~proposal.tacCategory.map(_.value()))
    val keywords = proposal.keywords.map(k => new Gsa.Keyword(k.value())).asJava
    val pi       = gsaPhase1DataInvestigator(proposal.investigators.pi)
    val cois     = proposal.investigators.cois.map(gsaPhase1DataInvestigator).asJava
    new Gsa(abstrakt, category, keywords, pi, cois)
  }

  private def gsaPhase1DataInvestigator(inv: Investigator): Gsa.Investigator =
    new Gsa.Investigator(inv.firstName, inv.lastName, inv.email)
}

package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}
import java.util.{TimeZone, GregorianCalendar}

trait Submissions // for ant

object PartnerSubmission {

//  // Lenses
//  def partner[A]:Lens[PartnerSubmission[A], A] = Lens(a => a.partner, (a, b) => a.copy(partner = b))
//  def request[A]:Lens[PartnerSubmission[A], SubmissionRequest] = Lens(a => a.request, (a, b) => a.copy(request = b))
//  def response[A]:Lens[PartnerSubmission[A], Option[SubmissionResponse]] = Lens(a => a.response, (a, b) => a.copy(response = b))



}

sealed trait Submission {
  def request:SubmissionRequest
  def response:Option[SubmissionResponse]
}

object Submission {
  def sumTimes(subs: Traversable[Submission], f: Submission => TimeAmount): TimeAmount =
    TimeAmount.sum(subs.map(f))
}

sealed trait PartnerSubmission[A, B <: PartnerSubmission[A,B]] extends Submission { this:B =>
  def partner:A
  def partnerLead:InvestigatorRef
  def reset:B
}

object NgoSubmission {

  def apply(m:M.NgoSubmission):NgoSubmission = apply(
    SubmissionRequest(m.getRequest),
    Option(m.getResponse).map(SubmissionResponse(_)),
    m.getPartner,
    Option(m.getPartnerLead).map(Investigator(_).ref).getOrElse(InvestigatorRef.empty))

}

case class NgoSubmission(request:SubmissionRequest,
                         response:Option[SubmissionResponse],
                         partner:NgoPartner,
                         partnerLead:InvestigatorRef) extends PartnerSubmission[NgoPartner, NgoSubmission] {

  def mutable(p:Proposal, n:Namer) = {
    val m = Factory.createNgoSubmission
    m.setPartner(partner)
    m.setRequest(request.mutable)
    m.setResponse(response.map(_.mutable).orNull)
    partnerLead(p).map(_.mutable(n)).foreach(m.setPartnerLead)
    m
  }

  def reset = copy(response = None)

}

object ExchangeSubmission {

  def apply(m:M.ExchangeSubmission):ExchangeSubmission = apply(
    SubmissionRequest(m.getRequest),
    Option(m.getResponse).map(SubmissionResponse(_)),
    m.getPartner,
    Option(m.getPartnerLead).map(Investigator(_).ref).getOrElse(InvestigatorRef.empty))

}

case class ExchangeSubmission(request:SubmissionRequest,
                              response:Option[SubmissionResponse],
                              partner:ExchangePartner,
                              partnerLead:InvestigatorRef) extends PartnerSubmission[ExchangePartner, ExchangeSubmission] {

  def mutable(p:Proposal, n:Namer) = {
    val m = Factory.createExchangeSubmission
    m.setPartner(partner)
    m.setRequest(request.mutable)
    m.setResponse(response.map(_.mutable).orNull)
    partnerLead(p).map(_.mutable(n)).foreach(m.setPartnerLead)
    m
  }

  def reset = copy(response = None)

}

object SpecialSubmission {

  // Lenses
  val specialType:Lens[SpecialSubmission, SpecialProposalType] = Lens.lensu((a, b) => a.copy(specialType = b), _.specialType)
  val request:Lens[SpecialSubmission, SubmissionRequest] = Lens.lensu((a, b) => a.copy(request = b), _.request)
  val response:Lens[SpecialSubmission, Option[SubmissionResponse]] = Lens.lensu((a, b) => a.copy(response = b), _.response)

  def apply(m:M.SpecialSubmission):SpecialSubmission = apply(
    SubmissionRequest(m.getRequest),
    Option(m.getResponse).map(SubmissionResponse(_)),
    m.getType
  )

  val empty = apply(SubmissionRequest.empty, None, SpecialProposalType.DEMO_SCIENCE)

}

case class SpecialSubmission(request:SubmissionRequest,
                             response:Option[SubmissionResponse],
                             specialType:SpecialProposalType) extends Submission {

  def mutable:M.SpecialSubmission = {
    val m = Factory.createSpecialSubmission
    m.setRequest(request.mutable)
    m.setResponse(response.map(_.mutable).orNull)
    m.setType(specialType)
    m
  }

  def reset:SpecialSubmission = copy(response = None)
}

case class LargeProgramSubmission(request:SubmissionRequest,
                             response:Option[SubmissionResponse]) extends Submission {

  def mutable:M.LargeProgramSubmission = {
    val m = Factory.createLargeProgramSubmission
    m.setRequest(request.mutable)
    m.setResponse(response.map(_.mutable).orNull)
    m
  }

  def reset:LargeProgramSubmission = copy(response = None)
}

object SubaruIntensiveProgramSubmission {
  // lenses
  val request: Lens[SubaruIntensiveProgramSubmission, SubmissionRequest] = Lens.lensu((a, b) => a.copy(request = b), _.request)
  val response: Lens[SubaruIntensiveProgramSubmission, Option[SubmissionResponse]] = Lens.lensu((a, b) => a.copy(response = b), _.response)

  def apply(m: M.SubaruIntensiveProgramSubmission): SubaruIntensiveProgramSubmission = apply(
    SubmissionRequest(m.getRequest),
    Option(m.getResponse).map(SubmissionResponse(_))
  )

  val empty = apply(SubmissionRequest.empty, None)

}

case class SubaruIntensiveProgramSubmission(request: SubmissionRequest,
                             response: Option[SubmissionResponse]) extends Submission {

  def mutable: M.SubaruIntensiveProgramSubmission = {
    val m = Factory.createSubaruIntensiveProgramSubmission
    m.setRequest(request.mutable)
    m.setResponse(response.map(_.mutable).orNull)
    m
  }

  def reset: SubaruIntensiveProgramSubmission = copy(response = None)
}

object LargeProgramSubmission {
  // lenses
  val request:Lens[LargeProgramSubmission, SubmissionRequest] = Lens.lensu((a, b) => a.copy(request = b), _.request)
  val response:Lens[LargeProgramSubmission, Option[SubmissionResponse]] = Lens.lensu((a, b) => a.copy(response = b), _.response)

  def apply(m:M.LargeProgramSubmission):LargeProgramSubmission = apply(
    SubmissionRequest(m.getRequest),
    Option(m.getResponse).map(SubmissionResponse(_))
  )

  val empty = apply(SubmissionRequest.empty, None)

}

case class FastTurnaroundSubmission(request            :SubmissionRequest,
                                    response           :Option[SubmissionResponse]) extends Submission {

  def mutable:M.FastTurnaroundSubmission = {
    val m = Factory.createFastTurnaroundSubmission
    m.setRequest(request.mutable)
    m.setResponse(response.map(_.mutable).orNull)
    m
  }

  def reset:FastTurnaroundSubmission = copy(response = None)

}

object FastTurnaroundSubmission {
  // lenses
  val request:Lens[FastTurnaroundSubmission, SubmissionRequest] = Lens.lensu((a, b) => a.copy(request = b), _.request)
  val response:Lens[FastTurnaroundSubmission, Option[SubmissionResponse]] = Lens.lensu((a, b) => a.copy(response = b), _.response)

  def apply(m:M.FastTurnaroundSubmission):FastTurnaroundSubmission = apply(
    SubmissionRequest(m.getRequest),
    Option(m.getResponse).map(SubmissionResponse(_))
  )

  val empty = apply(SubmissionRequest.empty, None)

}

object SubmissionRequest {

  // Lenses
  val time:Lens[SubmissionRequest, TimeAmount] = Lens.lensu((a, b) => a.copy(time = b), _.time)
  val minTime:Lens[SubmissionRequest, TimeAmount] = Lens.lensu((a, b) => a.copy(minTime = b), _.minTime)
  val totalLPTime:Lens[SubmissionRequest, Option[TimeAmount]] = Lens.lensu((a, b) => a.copy(totalLPTime= b), _.totalLPTime)
  val minTotalLPTime:Lens[SubmissionRequest, Option[TimeAmount]] = Lens.lensu((a, b) => a.copy(minTotalLPTime= b), _.minTotalLPTime)

  def apply(m:M.SubmissionRequest):SubmissionRequest = SubmissionRequest(
    TimeAmount(m.getTime),
    TimeAmount(m.getMinTime),
    Option(m.getTotalLPTime).map(TimeAmount.apply),
    Option(m.getMinTotalLPTime).map(TimeAmount.apply))

  lazy val empty = SubmissionRequest(TimeAmount.empty, TimeAmount.empty, None, None)

}

case class SubmissionRequest(time:TimeAmount, minTime:TimeAmount, totalLPTime: Option[TimeAmount], minTotalLPTime: Option[TimeAmount]) {

  def mutable = {
    val m = Factory.createSubmissionRequest
    m.setTime(time.mutable)
    m.setMinTime(minTime.mutable)
    totalLPTime.map(_.mutable).foreach(m.setTotalLPTime)
    minTotalLPTime.map(_.mutable).foreach(m.setMinTotalLPTime)
    m
  }

}

object SubmissionResponse {

  val decision:Lens[SubmissionResponse, Option[SubmissionDecision]] = Lens.lensu((a, b) => a.copy(decision = b), _.decision)
  val comment:Lens[SubmissionResponse, Option[String]] = Lens.lensu((a, b) => a.copy(comment = b), _.comment)

  /** Reads the SubmissionAccept, if any, but writes only a SubmissionAccept; writing None is a no-op */
  val acceptDecision:Lens[SubmissionResponse, Option[SubmissionAccept]] = Lens.lensu((r, b) => b match {
      case Some(a) => r.copy(decision = Some(SubmissionDecision(Right(a))))
      case _ =>
        println("got " + b + "; discarding")
        r // no-op
    }, _.decision match {
      case Some(SubmissionDecision(Right(a))) => Some(a)
      case _ => None
    })


  def apply(m:M.SubmissionResponse):SubmissionResponse = SubmissionResponse(
    SubmissionReceipt(m.getReceipt),
    SubmissionDecision(m),
    Option(m.getComment)
  )
}

case class SubmissionResponse(receipt:SubmissionReceipt,
                              decision:Option[SubmissionDecision] = None,
                              comment:Option[String] = None) {
  def mutable = {
    val m = Factory.createSubmissionResponse
    m.setReceipt(receipt.mutable)
    decision foreach {
      _.update(m)
    }
    m.setComment(comment.orNull)
    m
  }
}

object SubmissionReceipt {
  private val timeFactory = DatatypeFactory.newInstance()
  private val timeZone = TimeZone.getTimeZone("UTC")
  private def toXmlCalendar(timestamp:Long):XMLGregorianCalendar = {
    val gc = new GregorianCalendar(timeZone)
    gc.setTimeInMillis(timestamp)
    timeFactory.newXMLGregorianCalendar(gc)
  }

  // Lenses
  val id:Lens[SubmissionReceipt, String] = Lens.lensu((a, b) => a.copy(id = b), _.id)
  val timestamp:Lens[SubmissionReceipt, Long] = Lens.lensu((a, b) => a.copy(timestamp = b), _.timestamp)
  val contact:Lens[SubmissionReceipt, Option[String]] = Lens.lensu((a, b) => a.copy(contact = b), _.contact)

  def apply(m:M.SubmissionReceipt):SubmissionReceipt = SubmissionReceipt(
    m.getId,
    m.getTimestamp.toGregorianCalendar.getTimeInMillis,
    Option(m.getContact)
  )
}


case class SubmissionReceipt(id:String, timestamp:Long, contact: Option[String]) {
  def mutable = {
    val m = Factory.createSubmissionReceipt
    m.setId(id)
    m.setTimestamp(SubmissionReceipt.toXmlCalendar(timestamp))
    contact foreach { c => m.setContact(c) }
    m
  }
}

object SubmissionAccept {

  lazy val empty = apply("", 0.0, TimeAmount.empty, TimeAmount.empty, false)

  val email:Lens[SubmissionAccept, String] = Lens.lensu((a, b) => a.copy(email = b), _.email)

  def apply(m:M.SubmissionAccept):SubmissionAccept = SubmissionAccept(
    m.getEmail,
    m.getRanking.doubleValue,
    TimeAmount(m.getRecommend),
    TimeAmount(m.getMinRecommend),
    m.isPoorWeather
  )
}

case class SubmissionAccept(email:String, ranking:Double, recommended:TimeAmount, minRecommended:TimeAmount, poorWeather:Boolean) {
  def mutable = {
    val m = Factory.createSubmissionAccept
    m.setEmail(email)
    m.setRanking(java.math.BigDecimal.valueOf(ranking))
    m.setRecommend(recommended.mutable)
    m.setMinRecommend(minRecommended.mutable)
    m.setPoorWeather(poorWeather)
    m
  }
}

sealed trait SubmissionReject {
  def mutable:M.SubmissionReject
}

case object SubmissionReject extends SubmissionReject {
  def mutable = Factory.createSubmissionReject
}

object SubmissionDecision {
  def apply(m:M.SubmissionResponse):Option[SubmissionDecision] =
    (Option(m.getReject), Option(m.getAccept)) match {
      case (None, None) => None
      case (_, Some(a)) => Some(SubmissionDecision(Right(SubmissionAccept(a))))
      case (Some(_), _) => Some(SubmissionDecision(Left(SubmissionReject)))
    }
}

case class SubmissionDecision(decision:Either[SubmissionReject, SubmissionAccept]) {
  def update(m:M.SubmissionResponse) {
    m.setAccept(decision.right.map(_.mutable).right.getOrElse(null))
    m.setReject(decision.left.map(_.mutable).left.getOrElse(null))
  }
}


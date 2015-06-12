package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }
import scala.collection.JavaConverters._
import java.util.UUID

object Investigator {
  def apply(m:M.Investigator) = m match {
    case pi:M.PrincipalInvestigator => PrincipalInvestigator.apply(pi)
    case coi:M.CoInvestigator       => CoInvestigator.apply(coi)
  }

  //val noneID = UUID.randomUUID()
  //val none = CoInvestigator(noneID, "None", "", Nil, "", InvestigatorStatus.OTHER, "")
}

sealed trait Investigator {

  /**
   * Each Investigator has a UUID that can be used as a reference. This value is preserved on copy (unless a new one
   * is specified) and on PI/CoI conversion. Investigators constructed from the same mutable instance will have the
   * same uuid.
   */
  def uuid:UUID

  def ref:InvestigatorRef = InvestigatorRef(this)
  
  def firstName: String
  def lastName: String
  def email: String
  def phone: List[String]
  def status: InvestigatorStatus
  def mutable(n:Namer):M.Investigator

  def fullName = firstName + " " + lastName
  override def toString = fullName

  def toPi: PrincipalInvestigator
  def toCoi: CoInvestigator

  def isComplete:Boolean =
    EmailRegex.findFirstIn(email).isDefined &&
    List(firstName, lastName, email).forall(!_.trim.isEmpty) // phone is optional
}

object PrincipalInvestigator extends UuidCache[M.PrincipalInvestigator] {

  def apply(m: M.PrincipalInvestigator):PrincipalInvestigator = apply(
      uuid(m),
      m.getFirstName,
      m.getLastName,
      m.getPhone.asScala.toList,
      m.getEmail,
      m.getStatus,
      InstitutionAddress(m.getAddress))

  def empty = apply(UUID.randomUUID(), "", "", Nil, "", InvestigatorStatus.PH_D, InstitutionAddress.empty)

}

case class PrincipalInvestigator(
  uuid:UUID,
  firstName: String,
  lastName: String,
  phone: List[String],
  email: String,
  status: InvestigatorStatus,
  address: InstitutionAddress) extends Investigator {

  def mutable(n:Namer) = {
    val m = Factory.createPrincipalInvestigator
    m.setId(n.nameOf(this))
    m.setFirstName(firstName)
    m.setLastName(lastName)
    m.getPhone.addAll(phone.asJavaCollection)
    m.setEmail(email)
    m.setStatus(status)
    m.setAddress(address.mutable)
    m
  }

  def toPi  = this
  def toCoi = CoInvestigator(
    uuid,
    firstName,
    lastName,
    phone,
    email,
    status,
    address.institution)

  override def isComplete = super.isComplete && address.isComplete

}

object CoInvestigator extends UuidCache[M.CoInvestigator] {

  def apply(m: M.CoInvestigator):CoInvestigator  = apply(
      uuid(m),
      m.getFirstName,
      m.getLastName,
      m.getPhone.asScala.toList,
      m.getEmail,
      m.getStatus,
      m.getInstitution)

  def empty = apply(UUID.randomUUID(), "", "", Nil, "", InvestigatorStatus.PH_D, "")

}

case class CoInvestigator(
  uuid:UUID,
  firstName: String,
  lastName: String,
  phone: List[String],
  email: String,
  status: InvestigatorStatus,
  institution: String) extends Investigator {

  def mutable(n:Namer) = {
    val m = Factory.createCoInvestigator
    m.setId(n.nameOf(this))
    m.setFirstName(firstName)
    m.setLastName(lastName)
    m.getPhone.addAll(phone.asJavaCollection)
    m.setEmail(email)
    m.setStatus(status)
    m.setInstitution(institution)
    m
  }

  def toPi = PrincipalInvestigator(
    uuid,
    firstName,
    lastName,
    phone,
    email,
    status,
    InstitutionAddress.empty.copy(institution = institution))

  def toCoi  = this

  override def isComplete = super.isComplete && (!institution.trim.isEmpty)

}
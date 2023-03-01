package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

import scala.collection.JavaConverters._
import java.util.UUID

object Investigator {
  def apply(m:M.Investigator): Investigator = m match {
    case pi:M.PrincipalInvestigator => PrincipalInvestigator.apply(pi)
    case coi:M.CoInvestigator       => CoInvestigator.apply(coi)
  }

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
  def gender: InvestigatorGender
  def mutable(n:Namer):M.Investigator

  def fullName: String = s"$firstName $lastName"
  override def toString: String = fullName

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
      m.getGender,
      InstitutionAddress(m.getAddress))

  def empty = apply(UUID.randomUUID(), "", "", Nil, "", InvestigatorStatus.PH_D, InvestigatorGender.NONE_SELECTED, InstitutionAddress.empty)

}

case class PrincipalInvestigator(
  uuid:UUID,
  firstName: String,
  lastName: String,
  phone: List[String],
  email: String,
  status: InvestigatorStatus,
  gender: InvestigatorGender,
  address: InstitutionAddress) extends Investigator {

  def mutable(n:Namer): M.PrincipalInvestigator = {
    val m = Factory.createPrincipalInvestigator
    m.setId(n.nameOf(this))
    m.setFirstName(firstName)
    m.setLastName(lastName)
    m.getPhone.addAll(phone.asJavaCollection)
    m.setEmail(email)
    m.setStatus(status)
    m.setGender(gender)
    m.setAddress(address.mutable)
    m
  }

  def toPi: PrincipalInvestigator = this
  def toCoi = CoInvestigator(
    uuid,
    firstName,
    lastName,
    phone,
    email,
    status,
    gender,
    address.institution)

  override def isComplete: Boolean = super.isComplete && address.isComplete

}

object CoInvestigator extends UuidCache[M.CoInvestigator] {

  def apply(m: M.CoInvestigator):CoInvestigator  = apply(
      uuid(m),
      m.getFirstName,
      m.getLastName,
      m.getPhone.asScala.toList,
      m.getEmail,
      m.getStatus,
      m.getGender,
      m.getInstitution)

  def empty = apply(UUID.randomUUID(), "", "", Nil, "", InvestigatorStatus.PH_D, InvestigatorGender.NONE_SELECTED, "")

}

case class CoInvestigator(
  uuid:UUID,
  firstName: String,
  lastName: String,
  phone: List[String],
  email: String,
  status: InvestigatorStatus,
  gender: InvestigatorGender,
  institution: String) extends Investigator {

  def mutable(n:Namer): M.CoInvestigator = {
    val m = Factory.createCoInvestigator
    m.setId(n.nameOf(this))
    m.setFirstName(firstName)
    m.setLastName(lastName)
    m.getPhone.addAll(phone.asJavaCollection)
    m.setEmail(email)
    m.setStatus(status)
    m.setGender(gender)
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
    gender,
    InstitutionAddress.empty.copy(institution = institution))

  def toCoi: CoInvestigator = this

  override def isComplete: Boolean = super.isComplete && (!institution.trim.isEmpty)

}

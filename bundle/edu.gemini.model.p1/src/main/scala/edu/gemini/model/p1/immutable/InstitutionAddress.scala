package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object InstitutionAddress {
  def empty = apply("", "", "")
  def apply(name: String): InstitutionAddress = InstitutionAddress(name, "", "")
  def apply(m: M.InstitutionAddress) = new InstitutionAddress(m)
}

case class InstitutionAddress(
  institution: String,
  address: String,
  country: String) {

  def this(m: M.InstitutionAddress) = this(
    m.getInstitution,
    m.getAddress.trimLines,
    m.getCountry)

  def mutable = {
    val m = Factory.createInstitutionAddress
    m.setInstitution(institution)
    m.setAddress(address)
    m.setCountry(country)
    m
  }

  def isComplete = Seq(institution, country).map(_.trim).forall(!_.isEmpty) // address is optional
  
}
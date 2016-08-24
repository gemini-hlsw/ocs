package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable.{ExchangePartner, InstitutionAddress, NgoPartner}
import edu.gemini.model.p1.immutable.Partners.FtPartner

import scalaz.{-\/, \/-}
import xml.{Node, XML}

object Institutions {
  lazy val all: List[Institution] = {
    val xml = XML.load(getClass.getResource("institutions.xml"))
    val res = (xml \ "site").toList map { toInstitution }
    res.sortBy(_.name)
  }

  private def strList(n: Node, tag: String): List[String] =
    (n \ tag).map(_.text).toList

  private def toInstitution(n: Node): Institution = {
    val name    = (n \ "institution").text
    val address = strList(n, "address")
    val country = (n \ "country").text

    val contact = (n \ "contact").toList match {
      case h :: _ => toContact(h)
      case _ => Contact.empty
    }

    Institution(name, address, country, contact)
  }

  private def toContact(n: Node): Contact = {
    val phone  = strList(n, "phone")
    val email  = strList(n, "email")
    Contact(phone, email)
  }

  private def matchString(s: String): String = s.toLowerCase.collect {
    case x if x != '(' && x != ')' => x
  }

  def matchName(n: String): List[Institution] =
    all.filter(inst => matchString(inst.name).contains(matchString(n)))

  def bestMatch(n: String): Option[Institution] = matchName(n) match {
    case i :: _ => Some(i)
    case _      => None
  }

  def institution2Ngo(address: InstitutionAddress): FtPartner = {
    val geminiRegex = "Gemini.Observatory.*".r
    address.institution match {
      case geminiRegex() => Some(-\/(NgoPartner.US)) // Gemini Staff always go as US
      case _             => country2Ngo(address.country)
    }
  }

  def country2Ngo(country: String): FtPartner = country match {
    case "Argentina"         => Some(-\/(NgoPartner.AR))
    case "Australia"         => Some(-\/(NgoPartner.AU))
    case "Brazil"            => Some(-\/(NgoPartner.BR))
    case "Canada"            => Some(-\/(NgoPartner.CA))
    case "Chile"             => Some(-\/(NgoPartner.CL))
    case "Republic of Korea" => Some(-\/(NgoPartner.KR))
    case "USA"               => Some(-\/(NgoPartner.US))
    case "Japan"             => Some(\/-(ExchangePartner.SUBARU))
    case _                   => None
  }
}

object Contact {
  val empty = Contact(Nil, Nil)
}
case class Contact(phone: List[String], email: List[String])

object Institution {
  val empty = Institution("", Nil, "", Contact.empty)
}
case class Institution(name: String, addr: List[String], country: String, contact: Contact)

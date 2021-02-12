package edu.gemini.model.p1.immutable

import java.util.Locale

import scalaz.{-\/, Equal, \/, \/-}

object Partners {

  val name = Map[Any, String](
    NgoPartner.AR          -> "Argentina",
    NgoPartner.BR          -> "Brazil",
    NgoPartner.CA          -> "Canada",
    NgoPartner.CL          -> "Chile",
    NgoPartner.KR          -> "Republic of Korea",
    NgoPartner.US          -> "United States",
    NgoPartner.UH          -> "University of Hawaii",
    ExchangePartner.CFH    -> Site.CFH.name,
    ExchangePartner.KECK   -> Site.Keck.name,
    ExchangePartner.SUBARU -> Site.Subaru.name,
    LargeProgramPartner    -> "Large Program"
  )

  // REL-2670 A partner affiliation for an FT proposal can be either Ngo or Subaru
  type FtPartner = Option[NgoPartner \/ ExchangePartner.SUBARU.type]

  implicit val ftPartnerEqual = Equal.equalA[FtPartner]

  val NoPartnerAffiliation = "None"

  private val SubaruAffiliation = "Japan"

  // Possible FT Partners: None, Ngos, Subaru
  val ftPartners:Seq[(FtPartner, String)] = {
    (None -> NoPartnerAffiliation) :: (NgoPartner.values.filter(_ != NgoPartner.CL).toList.map(p => Option(-\/(p)) -> Partners.name.getOrElse(p, "")) ::: List((Option(\/-(ExchangePartner.SUBARU)): FtPartner) -> SubaruAffiliation)).sortBy(_._2)
  }

  /**
    * Returns the public name of the FT Affiliation Partner
    */
  def nameOfFTPartner(fp: FtPartner): Option[String] = fp match {
    case Some(-\/(p))                      => Partners.name.get(p)
    case Some(\/-(ExchangePartner.SUBARU)) => Some(SubaruAffiliation)
    case _                                 => None
  }

  def toPartner(name: String): FtPartner = ftPartners.find(_._2 == name).collect {
    case (Some(a), _) => a
  }

  def forLocale(loc:Locale):Option[Either[NgoPartner, ExchangePartner]] = loc match {
    case Locale.US    => None // ambiguous; could be US, UH, or KECK
    case Locale.JAPAN => Some(Right(ExchangePartner.SUBARU))
    case _            => NgoPartner.values.find(_.value.equalsIgnoreCase(loc.getCountry)).map(Left(_))
  }
}
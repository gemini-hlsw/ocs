package edu.gemini.model.p1.immutable

import java.util.Locale

import scalaz.{-\/, \/, \/-}

object Partners {

  val name = Map[Any, String](
    NgoPartner.AR -> "Argentina",
    NgoPartner.AU -> "Australia",
    NgoPartner.BR -> "Brazil",
    NgoPartner.CA -> "Canada",
    NgoPartner.CL -> "Chile",
    NgoPartner.KR -> "Korea",
    NgoPartner.US -> "United States",
    NgoPartner.UH -> "University of Hawaii",
    ExchangePartner.CFHT -> Site.CFHT.name,
    ExchangePartner.KECK -> Site.Keck.name,
    ExchangePartner.SUBARU -> Site.Subaru.name,
    LargeProgramPartner -> "Large Program"
  )

  // REL-2248 Contains a list of partners that are not allowed on joint proposals
  val jointProposalNotAllowed = List[NgoPartner](NgoPartner.KR, NgoPartner.AU)

  // REL-2670 A partner for FT can be either Ngo or Exchange
  type FtPartner = Option[NgoPartner \/ ExchangePartner]

  val NoPartnerAffiliation = "None"

  val ftPartners:Seq[(FtPartner, String)] = {
    (None -> NoPartnerAffiliation) :: (NgoPartner.values.toList.map(p => Option(-\/(p)) -> Partners.name.getOrElse(p, "")) ::: List(Option(\/-(ExchangePartner.SUBARU)) -> "Japan")).sortBy(_._2)
  }

  def toPartner(name: String): FtPartner = ftPartners.find(_._2 == name).collect {
    case (Some(-\/(p)), _) => -\/(p)
    case (Some(\/-(e)), _) => \/-(e)
  }

  def forLocale(loc:Locale):Option[Either[NgoPartner, ExchangePartner]] = loc match {
    case Locale.US    => None // ambiguous; could be US, UH, or KECK
    case Locale.JAPAN => Some(Right(ExchangePartner.SUBARU))
    case _            => NgoPartner.values.find(_.value.equalsIgnoreCase(loc.getCountry)).map(Left(_))
  }
}
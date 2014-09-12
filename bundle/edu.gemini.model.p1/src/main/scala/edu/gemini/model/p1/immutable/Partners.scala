package edu.gemini.model.p1.immutable

import java.util.Locale

object Partners {

  val name = Map[Any, String](
    NgoPartner.AR -> "Argentina",
    NgoPartner.AU -> "Australia",
    NgoPartner.BR -> "Brazil",
    NgoPartner.CA -> "Canada",
    NgoPartner.CL -> "Chile",
    NgoPartner.US -> "United States",
    NgoPartner.UH -> "University of Hawaii",
    ExchangePartner.KECK -> Site.Keck.name,
    ExchangePartner.SUBARU -> Site.Subaru.name,
    LargeProgramPartner -> "Large Program"
  )

  val ftPartners:Seq[(Option[NgoPartner], String)] = {
    (None -> "None") :: NgoPartner.values.toList.map(p => Some(p) -> Partners.name.getOrElse(p, ""))
  }

  def toPartner(name: String): Option[NgoPartner] = Partners.name.find(_._2 == name).collect {
    case (p: NgoPartner, _) => p
  }

  def forLocale(loc:Locale):Option[Either[NgoPartner, ExchangePartner]] = loc match {
    case Locale.US    => None // ambiguous; could be US, UH, or KECK
    case Locale.JAPAN => Some(Right(ExchangePartner.SUBARU))
    case _            => NgoPartner.values.find(_.value.equalsIgnoreCase(loc.getCountry)).map(Left(_))
  }
}
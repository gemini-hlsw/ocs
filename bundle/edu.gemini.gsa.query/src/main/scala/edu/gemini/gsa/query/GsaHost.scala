package edu.gemini.gsa.query

import java.net.URL

import scalaz._

/** Defines GsaHost options.  There is a GSA server at each summit and then
  * there is the public archive server.  The Data Manger works with both
  * instances and needs to distinguish the two.
  */
sealed trait GsaHost {
  def host: String
  def protocol: String

  def baseUrl = s"$protocol://$host"
}

object GsaHost {
  final case class Summit(host: String) extends GsaHost {
    override val protocol: String = "http"
  }

  final case class Archive(host: String) extends GsaHost {
    override val protocol: String = "https"
  }

  implicit val EqualGsaHost: Equal[GsaHost] = Equal.equalA
}

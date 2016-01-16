package edu.gemini.gsa.client.impl

import edu.gemini.gsa.client.api.{GsaUnsupportedParams, GsaNonSiderealParams, GsaSiderealParams, GsaParams}
import java.net.URL

import edu.gemini.gsa.query.GsaHost

object GsaUrl {
  val ROOT = GsaHost.Archive("archive.gemini.edu")
  val siderealPrefix = s"${ROOT.baseUrl}/searchform/notengineering/science/NotFail/OBJECT"
  val nonSiderealPrefix = s"${ROOT.baseUrl}/searchform"

  /**
   * Converts the GsaParams into a URL that can be used to query the GSA.
   */
  def apply(params: GsaParams): URL = {
    def siderealUrl(filter: String, instrumentName: String): URL = new URL(s"$siderealPrefix/$instrumentName/$filter")
    def nonSiderealUrl(filter: String, instrumentName: String): URL = new URL(s"$nonSiderealPrefix/$filter/$instrumentName/NotFail")

    params match {
      case GsaSiderealParams(coords, instrument)        => siderealUrl(s"ra=${coords.ra.toAngle.toDegrees}/dec=${coords.dec.toDegrees}/sr=60", instrument.name)
      case GsaNonSiderealParams(targetName, instrument) => nonSiderealUrl(s"object=$targetName/notengineering/", instrument.name)
      case GsaUnsupportedParams                         => new URL(ROOT.baseUrl)
    }
  }
}

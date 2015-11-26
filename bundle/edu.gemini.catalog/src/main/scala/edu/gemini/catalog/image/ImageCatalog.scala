package edu.gemini.catalog.image

import java.net.URL

import edu.gemini.spModel.core.{Angle, Coordinates}
import jsky.util.Preferences

sealed abstract class ImageCatalog(val id: String, displayName: String) {
  def queryUrl(c: Coordinates, w: Angle): URL
}

object DssGeminiNorth extends ImageCatalog("dss@GeminiNorth", "Digitized Sky at Gemini North") {
  def queryUrl(c: Coordinates, w: Angle): URL = new URL("http://mkocatalog.gemini.edu/cgi-bin/dss_search?ra=%ra&dec =% dec & mime - type =% mime - type & x =% w & y =% h")
}
object DssGeminiSouth extends ImageCatalog("dss@GeminiSouth", "Digitized Sky at Gemini South") {
  def queryUrl(c: Coordinates, w: Angle): URL = new URL("http://cpocatalog.gemini.edu/cgi-bin/dss_search?ra=%ra&dec=%dec&mime-type=%mime-type&x=%w&y=%h")
}
object DssESO extends ImageCatalog("dss@eso", "Digitized Sky at ESO") {
  def queryUrl(c: Coordinates, w: Angle): URL = new URL("http://archive.eso.org/dss/dss?ra=%ra&dec=%dec&mime-type=%mime-type&x=%w&y=%h")
}
object Dss2ESO extends ImageCatalog("dss2@eso", "Digitized Sky (Version II) at ESO") {
  def queryUrl(c: Coordinates, w: Angle): URL = new URL("http://archive.eso.org/dss/dss?ra=%ra&dec=%dec&mime-type=application/x-fits&x=%w&y=%h&Sky-Survey=DSS2")
}
object Dss2iESO extends ImageCatalog("dss2_i@GeminiNorth", "Digitized Sky (Version II infrared) at ESO") {
  def queryUrl(c: Coordinates, w: Angle): URL = new URL("http://archive.eso.org/dss/dss?ra=%ra&dec=%dec&mime-type=application/x-fits&x=%w&y=%h&Sky-Survey=DSS2")
}

object ImageCatalog {
  private val SKY_USER_CATALOG = "jsky.catalog.sky"

  val all = List(DssGeminiNorth, DssGeminiSouth, DssESO, Dss2ESO, Dss2iESO)

  val defaultImageServer = DssGeminiNorth

  def user = all.find(_.id == Preferences.get(SKY_USER_CATALOG, defaultImageServer.id)).getOrElse(defaultImageServer)

  def user(is: ImageCatalog) = Preferences.set(SKY_USER_CATALOG, is.id)
}

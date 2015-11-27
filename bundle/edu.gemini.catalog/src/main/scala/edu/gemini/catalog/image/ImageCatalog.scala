package edu.gemini.catalog.image

import java.net.URL

import edu.gemini.spModel.core.{Angle, Coordinates}
import jsky.util.Preferences

sealed abstract class ImageCatalog(val id: String, val displayName: String) {
  def baseUrl: String
  def extraParams: String = ""
  def queryUrl(c: Coordinates): URL = new URL(s"$baseUrl?ra=${c.ra.toAngle.formatHMS}&dec=${c.dec.formatDMS}&mime-type=application/x-fits&x=${ImageCatalog.defaultSize.toArcmins}&y=${ImageCatalog.defaultSize.toArcmins}${extraParams}")
}

object DssGeminiNorth extends ImageCatalog("dss@GeminiNorth", "Digitized Sky at Gemini North") {
  override val baseUrl: String = "http://mkocatalog.gemini.edu/cgi-bin/dss_search"
}
object DssGeminiSouth extends ImageCatalog("dss@GeminiSouth", "Digitized Sky at Gemini South") {
  override val baseUrl: String = "http://cpocatalog.gemini.edu/cgi-bin/dss_search"
}
object DssESO extends ImageCatalog("dss@eso", "Digitized Sky at ESO") {
  override val baseUrl: String = "http://archive.eso.org/dss/dss"
}
object Dss2ESO extends ImageCatalog("dss2@eso", "Digitized Sky (Version II) at ESO") {
  override val baseUrl: String = "http://archive.eso.org/dss/dss"
  override val extraParams = "&Sky-Survey=DSS2"
}
object Dss2iESO extends ImageCatalog("dss2_i@eso", "Digitized Sky (Version II infrared) at ESO") {
  override val baseUrl: String = "http://archive.eso.org/dss/dss"
  override val extraParams = "&Sky-Survey=DSS2-infrared"
}

object ImageCatalog {
  val instance = this

  val defaultSize = Angle.fromArcmin(15.0)

  private val SKY_USER_CATALOG = "jsky.catalog.sky"

  val all = List(DssGeminiNorth, DssGeminiSouth, DssESO, Dss2ESO, Dss2iESO)

  val defaultImageServer = DssGeminiNorth

  def user = all.find(_.id == Preferences.get(SKY_USER_CATALOG, defaultImageServer.id)).getOrElse(defaultImageServer)

  def user(is: ImageCatalog) = Preferences.set(SKY_USER_CATALOG, is.id)
}

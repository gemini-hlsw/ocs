package edu.gemini.catalog.image

import java.net.URL

import edu.gemini.spModel.core.{MagnitudeBand, Angle, Coordinates}
import jsky.util.Preferences

sealed abstract class ImageCatalog(val id: String, val displayName: String) {
  def queryUrl(c: Coordinates): URL
}
abstract class DssCatalog(id: String, displayName: String) extends ImageCatalog(id, displayName) {
  def baseUrl: String
  def extraParams: String = ""
  override def queryUrl(c: Coordinates): URL = new URL(s"$baseUrl?ra=${c.ra.toAngle.formatHMS}&dec=${c.dec.formatDMS}&mime-type=application/x-fits&x=${ImageCatalog.defaultSize.toArcmins}&y=${ImageCatalog.defaultSize.toArcmins}$extraParams")
}
abstract class AstroCatalog(id: String, displayName: String) extends ImageCatalog(id, displayName) {
  def band: MagnitudeBand
  override def queryUrl(c: Coordinates): URL = new URL(s" http://irsa.ipac.caltech.edu/cgi-bin/Oasis/2MASSImg/nph-2massimg?objstr=${c.ra.toAngle.formatHMS}%20${c.dec.formatDMS}&size=${ImageCatalog.defaultSize.toArcsecs}&band=${band.name}")
}

object DssGeminiNorth extends DssCatalog("dss@GeminiNorth", "Digitized Sky at Gemini North") {
  override val baseUrl: String = "http://mkocatalog.gemini.edu/cgi-bin/dss_search"
}
object DssGeminiSouth extends DssCatalog("dss@GeminiSouth", "Digitized Sky at Gemini South") {
  override val baseUrl: String = "http://cpocatalog.gemini.edu/cgi-bin/dss_search"
}
object DssESO extends DssCatalog("dss@eso", "Digitized Sky at ESO") {
  override val baseUrl: String = "http://archive.eso.org/dss/dss"
}
object Dss2ESO extends DssCatalog("dss2@eso", "Digitized Sky (Version II) at ESO") {
  override val baseUrl: String = "http://archive.eso.org/dss/dss"
  override val extraParams = "&Sky-Survey=DSS2"
}
object Dss2iESO extends DssCatalog("dss2_i@eso", "Digitized Sky (Version II infrared) at ESO") {
  override val baseUrl: String = "http://archive.eso.org/dss/dss"
  override val extraParams = "&Sky-Survey=DSS2-infrared"
}
object MassImgJ extends AstroCatalog("2massJ", "2MASS Quick-Look Image Retrieval Service (J Band)") {
  override val band = MagnitudeBand.J
}
object MassImgH extends AstroCatalog("2massH", "2MASS Quick-Look Image Retrieval Service (H Band)") {
  override val band = MagnitudeBand.H
}
object MassImgK extends AstroCatalog("2massK", "2MASS Quick-Look Image Retrieval Service (K Band)") {
  override val band = MagnitudeBand.K
}

object ImageCatalog {
  val instance = this

  val defaultSize = Angle.fromArcmin(15.0)

  private val SKY_USER_CATALOG = "jsky.catalog.sky"

  val all = List(DssGeminiNorth, DssGeminiSouth, DssESO, Dss2ESO, Dss2iESO, MassImgJ, MassImgH, MassImgK)

  val defaultImageServer = DssGeminiNorth

  def user = all.find(_.id == Preferences.get(SKY_USER_CATALOG, defaultImageServer.id)).getOrElse(defaultImageServer)

  def user(is: ImageCatalog) = Preferences.set(SKY_USER_CATALOG, is.id)
}

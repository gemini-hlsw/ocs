package edu.gemini.catalog.image

import java.net.URL

import edu.gemini.spModel.core.{Angle, Coordinates, MagnitudeBand, Wavelength}
import jsky.util.Preferences
import squants.information.Information
import squants.information.InformationConversions._
import edu.gemini.spModel.core.WavelengthConversions._

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

/** Represents an end point that can load an image for a given set of coordinates */
sealed abstract class ImageCatalog(val id: String, val displayName: String, val shortName: String) {
  /** Returns the url that can load the passed coordinates */
  def queryUrl(c: Coordinates): URL

  override def toString: String = id
}

/** Base class for DSS based image catalogs */
abstract class DssCatalog(id: String, displayName: String, shortName: String) extends ImageCatalog(id, displayName, shortName) {
  def baseUrl: String
  def extraParams: String = ""
  override def queryUrl(c: Coordinates): URL = new URL(s"$baseUrl?ra=${c.ra.toAngle.formatHMS}&dec=${c.dec.formatDMS}&mime-type=application/x-fits&x=${ImageCatalog.defaultSize.toArcmins}&y=${ImageCatalog.defaultSize.toArcmins}$extraParams")
}

/** Base class for 2MASSImg based image catalogs */
abstract class AstroCatalog(id: String, displayName: String, shortName: String) extends ImageCatalog(id, displayName, shortName) {
  def band: MagnitudeBand
  override def queryUrl(c: Coordinates): URL = new URL(s" http://irsa.ipac.caltech.edu/cgi-bin/Oasis/2MASSImg/nph-2massimg?objstr=${c.ra.toAngle.formatHMS}%20${c.dec.formatDMS}&size=${ImageCatalog.defaultSize.toArcsecs}&band=${band.name}")
}

// Concrete instances of image catalogs
object DssGeminiNorth extends DssCatalog("dss@GeminiNorth", "Digitized Sky at Gemini North", "DSS GN") {
  override val baseUrl: String = "http://mkocatalog.gemini.edu/cgi-bin/dss_search"
}

object DssGeminiSouth extends DssCatalog("dss@GeminiSouth", "Digitized Sky at Gemini South", "DSS GS") {
  override val baseUrl: String = "http://cpocatalog.gemini.edu/cgi-bin/dss_search"
}

object DssESO extends DssCatalog("dss@eso", "Digitized Sky at ESO", "DSS ESO") {
  override val baseUrl: String = "http://archive.eso.org/dss/dss"
}

object Dss2ESO extends DssCatalog("dss2@eso", "Digitized Sky (Version II) at ESO", "DSS ESO (II)") {
  override val baseUrl: String = "http://archive.eso.org/dss/dss"
  override val extraParams = "&Sky-Survey=DSS2"
}

object Dss2iESO extends DssCatalog("dss2_i@eso", "Digitized Sky (Version II infrared) at ESO", "DSS ESO (II IR)") {
  override val baseUrl: String = "http://archive.eso.org/dss/dss"
  override val extraParams = "&Sky-Survey=DSS2-infrared"
}

object MassImgJ extends AstroCatalog("2massJ", "2MASS Quick-Look Image Retrieval Service (J Band)", "2MASS-J") {
  override val band = MagnitudeBand.J
}

object MassImgH extends AstroCatalog("2massH", "2MASS Quick-Look Image Retrieval Service (H Band)", "2MASS-H") {
  override val band = MagnitudeBand.H
}

object MassImgK extends AstroCatalog("2massK", "2MASS Quick-Look Image Retrieval Service (K Band)", "2MASS-K") {
  override val band = MagnitudeBand.K
}

/**
  * Contains definitions for ImageCatalogs including a list of all the available image servers
  */
object ImageCatalog {
  val defaultSize: Angle = Angle.fromArcmin(15.0)
  /** Default image server */
  val DefaultImageServer = DssGeminiNorth

  implicit val equals = Equal.equalA[ImageCatalog]

  private val DssCutoff   = 1.0.microns
  private val MassJCutoff = 1.4.microns
  private val MassHCutoff = 1.9.microns

  /** List of all known image server in preference order */
  val all = List(DssGeminiNorth, DssGeminiSouth, DssESO, Dss2ESO, Dss2iESO, MassImgJ, MassImgH, MassImgK)

  def byName(id: String): Option[ImageCatalog] = all.find(_.id == id)

  /**
    * Returns the catalog appropriate for the given wavelength
    */
  def catalogForWavelength(w: Option[Wavelength]): ImageCatalog = w match {
    case Some(d) if d <= DssCutoff   => DssGeminiSouth
    case Some(d) if d <= MassJCutoff => MassImgJ
    case Some(d) if d <= MassHCutoff => MassImgH
    case Some(_)                     => MassImgK
    case None                        => DefaultImageServer
  }
}

/**
  * Preferences handling the image catalog, like cache size, etc
  * This are backed up to disk with the other jsky properties
  */
case class ImageCatalogPreferences(imageCacheSize: Information, defaultCatalog: ImageCatalog)

object ImageCatalogPreferences {
  val DefaultCacheSize: Information = 500.mb

  private val ImageDefaultCatalog = "ot.catalog.default"
  private val ImageMaxCacheSize = "ot.cache.size"

  val zero = ImageCatalogPreferences(DefaultCacheSize, ImageCatalog.DefaultImageServer)

  /**
    * Indicates the user preferences about Image Catalogs
    */
  def preferences(): Task[ImageCatalogPreferences] = Task.delay { // Inside task as it reads the preferences file
    \/.fromTryCatchNonFatal {
      // Try to parse preferences
      val size = Option(Preferences.get(ImageMaxCacheSize)).map(_.toDouble)
      val catalog = ImageCatalog.all.find(_.id == Preferences.get(ImageDefaultCatalog)).getOrElse(ImageCatalog.DefaultImageServer)

      ImageCatalogPreferences(size.map(_.megabytes).getOrElse(ImageCatalogPreferences.DefaultCacheSize), catalog)
    }.getOrElse(ImageCatalogPreferences.zero)
  }

  /**
    * Sets the user preferences about Image Catalogs
    */
  def preferences(prefs: ImageCatalogPreferences): Task[Unit] = Task.delay { // Inside task as it writes the preferences file
    Preferences.set(ImageMaxCacheSize, prefs.imageCacheSize.toMegabytes.toString)
    Preferences.set(ImageDefaultCatalog, prefs.defaultCatalog.id)
  }
}

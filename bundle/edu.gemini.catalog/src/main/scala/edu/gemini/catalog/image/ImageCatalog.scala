package edu.gemini.catalog.image

import java.net.URL

import edu.gemini.spModel.core.{Angle, Coordinates, MagnitudeBand, Wavelength}
import jsky.util.Preferences
import squants.information.Information
import squants.information.InformationConversions._
import edu.gemini.spModel.core.WavelengthConversions._

import scalaz._
import scalaz.NonEmptyList._
import Scalaz._
import scalaz.concurrent.Task

/** Represents an end point that can load an image for a given set of coordinates */
sealed abstract class ImageCatalog(val id: String, val displayName: String, val shortName: String) {
  /** Returns the urls that can load the passed coordinates */
  def queryUrl(c: Coordinates): NonEmptyList[URL]

  override def toString: String = id
}

/** Base class for DSS based image catalogs */
abstract class DssCatalog(id: String, displayName: String, shortName: String) extends ImageCatalog(id, displayName, shortName) {
  def baseUrl: NonEmptyList[String]
  def extraParams: String = ""
  override def queryUrl(c: Coordinates): NonEmptyList[URL] = baseUrl.map(u => new URL(s"$u?ra=${c.ra.toAngle.formatHMS}&dec=${c.dec.formatDMS}&mime-type=application/x-fits&x=${ImageCatalog.DefaultImageSize.toArcmins}&y=${ImageCatalog.DefaultImageSize.toArcmins}$extraParams"))
}

/** Base class for 2MASSImg based image catalogs */
abstract class AstroCatalog(id: String, displayName: String, shortName: String) extends ImageCatalog(id, displayName, shortName) {
  def band: MagnitudeBand
  override def queryUrl(c: Coordinates): NonEmptyList[URL] = NonEmptyList(new URL(s" http://irsa.ipac.caltech.edu/cgi-bin/Oasis/2MASSImg/nph-2massimg?objstr=${c.ra.toAngle.formatHMS}%20${c.dec.formatDMS}&size=${ImageCatalog.DefaultImageSize.toArcsecs}&band=${band.name}"))
}

// Concrete instances of image catalogs
/**
  * DSS at Gemini, distributes the load among MKO and CPO
  */
object DssGemini extends DssCatalog("dss@Gemini", "Digitized Sky at Gemini", "DSS Gemini") {
  override val baseUrl: NonEmptyList[String] = NonEmptyList("mko", "cpo").map(c => s"http://${c}catalog.gemini.edu/cgi-bin/dss_search")
}

object DssESO extends DssCatalog("dss@eso", "Digitized Sky at ESO", "DSS ESO") {
  override val baseUrl: NonEmptyList[String] = nels("http://archive.eso.org/dss/dss")
}

object Dss2ESO extends DssCatalog("dss2@eso", "Digitized Sky (Version II) at ESO", "DSS ESO (II)") {
  override val baseUrl: NonEmptyList[String] = nels("http://archive.eso.org/dss/dss")
  override val extraParams = "&Sky-Survey=DSS2"
}

object Dss2iESO extends DssCatalog("dss2_i@eso", "Digitized Sky (Version II infrared) at ESO", "DSS ESO (II IR)") {
  override val baseUrl: NonEmptyList[String] = nels("http://archive.eso.org/dss/dss")
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
  val DefaultImageSize: Angle = Angle.fromArcmin(15.0)
  val DefaultImageServer = DssGemini

  implicit val equals: Equal[ImageCatalog] = Equal.equalA[ImageCatalog]

  // Wavelength cutoffs to select a given catalog
  private val DssCutoff   = 1.0.microns
  private val MassJCutoff = 1.4.microns
  private val MassHCutoff = 1.9.microns

  /** List of all known image server in preference order */
  val all = List(DssGemini, DssESO, Dss2ESO, Dss2iESO, MassImgJ, MassImgH, MassImgK)

  def byName(id: String): Option[ImageCatalog] = all.find(_.id == id)

  /**
    * Returns a catalog appropriate for a given wavelength
    */
  def catalogForWavelength(w: Option[Wavelength]): ImageCatalog = w match {
    case Some(d) if d <= DssCutoff   => DssGemini
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
  def preferences(): Task[ImageCatalogPreferences] = Task.delay {
    \/.fromTryCatchNonFatal {
      // Try to parse preferences, Preferences.get read a file
      val size = Option(Preferences.get(ImageMaxCacheSize)).map(_.toDouble)
      val catalog = ImageCatalog.all.find(_.id == Preferences.get(ImageDefaultCatalog)).getOrElse(ImageCatalog.DefaultImageServer)

      ImageCatalogPreferences(size.map(_.megabytes).getOrElse(ImageCatalogPreferences.DefaultCacheSize), catalog)
    }.getOrElse(ImageCatalogPreferences.zero)
  }

  /**
    * Sets the user preferences about Image Catalogs
    */
  def preferences(prefs: ImageCatalogPreferences): Task[Unit] = Task.delay {
    // These calls write to a file
    Preferences.set(ImageMaxCacheSize, prefs.imageCacheSize.toMegabytes.toString)
    Preferences.set(ImageDefaultCatalog, prefs.defaultCatalog.id)
  }
}

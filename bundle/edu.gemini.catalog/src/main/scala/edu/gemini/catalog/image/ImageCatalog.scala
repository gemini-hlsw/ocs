package edu.gemini.catalog.image

import java.net.URL

import edu.gemini.spModel.core.{Angle, Coordinates, MagnitudeBand, Site, Wavelength}
import edu.gemini.spModel.core.WavelengthConversions._

import scalaz._
import scalaz.NonEmptyList._
import Scalaz._

sealed trait CatalogId {
  def filePrefix: String
}

case object DssGeminiId extends CatalogId {
  val filePrefix = "dssGemini"
}
case object DssGeminiSouthId extends CatalogId {
  val filePrefix = "dssGeminiSouth"
}
case object DssGeminiNorthId extends CatalogId {
  val filePrefix = "dssGeminiNorth"
}
case object DssEsoId extends CatalogId {
  val filePrefix = "dssEso"
}
case object Dss2EsoId extends CatalogId {
  val filePrefix = "dss2Eso"
}
case object Dss2IREsoId extends CatalogId {
  val filePrefix = "dss2iEso"
}
case object TwoMassJId extends CatalogId {
  val filePrefix = "2massJ"
}
case object TwoMassHId extends CatalogId {
  val filePrefix = "2massH"
}
case object TwoMassKId extends CatalogId {
  val filePrefix = "2massK"
}

object CatalogId {
  val all: List[CatalogId] = List(DssGeminiId, DssGeminiSouthId, DssGeminiNorthId, DssEsoId, Dss2EsoId, Dss2IREsoId, TwoMassJId, TwoMassHId, TwoMassKId)
}

/** Represents an end point that can load an image for a given set of coordinates */
sealed abstract class ImageCatalog(val id: CatalogId, val displayName: String, val shortName: String) {
  /** Returns the urls that can load the passed coordinates, site indicates the preferred site to use for the query */
  def queryUrl(c: Coordinates, site: Option[Site]): NonEmptyList[URL]

  /** Size of the default requested image */
  def imageSize: AngularSize

  /**
    * Overlap angle between adjacent images.
    * In certain catalogs images can overlap to a certain degree. In that case we would get multiple hits
    * for the same target. We use the value adjacentOverlap to correct this and request images on the overlap
    * area if there is another closer
    */
  // TODO Should be corrected for dec?
  def adjacentOverlap: Angle
}

/** Base class for DSS based image catalogs */
abstract class DssCatalog(id: CatalogId, displayName: String, shortName: String) extends ImageCatalog(id, displayName, shortName) {
  protected def baseUrl(site: Option[Site]): NonEmptyList[String]

  protected def extraParams: String = ""

  def imageSize: AngularSize = AngularSize(ImageCatalog.DefaultImageSize, ImageCatalog.DefaultImageSize)

  def adjacentOverlap: Angle = Angle.fromArcmin(3)

  override def queryUrl(c: Coordinates, site: Option[Site]): NonEmptyList[URL] =
    baseUrl(site).map(u => new URL(s"$u?ra=${c.ra.toAngle.formatHMS}&dec=${c.dec.formatDMS}&mime-type=application/x-fits&x=${imageSize.ra.toArcmins}&y=${imageSize.dec.toArcmins}$extraParams"))
}

/** Base class for 2MASSImg based image catalogs */
abstract class AstroCatalog(id: CatalogId, displayName: String, shortName: String) extends ImageCatalog(id, displayName, shortName) {
  def band: MagnitudeBand

  def imageSize: AngularSize = AngularSize(ImageCatalog.DefaultImageSize, ImageCatalog.DefaultImageSize)

  private val size = imageSize.ra.max(imageSize.dec)

  def adjacentOverlap: Angle = Angle.zero

  override def queryUrl(c: Coordinates, site: Option[Site]): NonEmptyList[URL] =
    NonEmptyList(new URL(s" http://irsa.ipac.caltech.edu/cgi-bin/Oasis/2MASSImg/nph-2massimg?objstr=${c.ra.toAngle.formatHMS}%20${c.dec.formatDMS}&size=${size.toArcsecs.toInt}&band=${band.name}"))
}

// Concrete instances of image catalogs
/**
  * DSS at Gemini, it communicates with both servers at GN and GS
  */
object DssGemini extends DssCatalog(DssGeminiId, "Digitized Sky at Gemini", "DSS Gemini") {
  override def baseUrl(site: Option[Site]): NonEmptyList[String] = site match {
    case Some(Site.GS) => nels("http://cpocatalog.gemini.edu/cgi-bin/dss_search")
    case Some(Site.GN) => nels("http://mkocatalog.gemini.edu/cgi-bin/dss_search")
    case _             => NonEmptyList("mko", "cpo").map(c => s"http://${c}catalog.gemini.edu/cgi-bin/dss_search")
  }
}
object DssESO extends DssCatalog(DssEsoId, "Digitized Sky at ESO", "DSS ESO") {
  override def baseUrl(site: Option[Site]): NonEmptyList[String] = nels("http://archive.eso.org/dss/dss")
}

object Dss2ESO extends DssCatalog(Dss2EsoId, "Digitized Sky (Version II) at ESO", "DSS ESO (II)") {
  override def baseUrl(site: Option[Site]): NonEmptyList[String] = nels("http://archive.eso.org/dss/dss")
  override val extraParams = "&Sky-Survey=DSS2"
}

object Dss2iESO extends DssCatalog(Dss2IREsoId, "Digitized Sky (Version II infrared) at ESO", "DSS ESO (II IR)") {
  override def baseUrl(site: Option[Site]): NonEmptyList[String] = nels("http://archive.eso.org/dss/dss")
  override val extraParams = "&Sky-Survey=DSS2-infrared"
}

object TwoMassJ extends AstroCatalog(TwoMassJId, "2MASS Quick-Look Image Retrieval Service (J Band)", "2MASS-J") {
  override val band = MagnitudeBand.J
}

object TwoMassH extends AstroCatalog(TwoMassHId, "2MASS Quick-Look Image Retrieval Service (H Band)", "2MASS-H") {
  override val band = MagnitudeBand.H
}

object TwoMassK extends AstroCatalog(TwoMassKId, "2MASS Quick-Look Image Retrieval Service (K Band)", "2MASS-K") {
  override val band = MagnitudeBand.K
}

/**
  * Contains definitions for ImageCatalogs including a list of all the available image servers
  */
object ImageCatalog {
  val DefaultImageSize: Angle = Angle.fromArcmin(15.0)
  val DefaultImageCatalog = DssGemini

  /** @group Typeclass Instances */
  implicit val equals: Equal[ImageCatalog] = Equal.equalA[ImageCatalog]

  // Wavelength cutoffs to select a given catalog
  private val DssCutoff   = 1.0.microns
  private val MassJCutoff = 1.4.microns
  private val MassHCutoff = 1.9.microns

  /** List of all known public image server in preference order */
  val all = List(DssGemini, DssESO, Dss2ESO, Dss2iESO, TwoMassJ, TwoMassH, TwoMassK)

  private val catalogsById = all.map(c => c.id.filePrefix -> c).toMap

  def byId(id: String): Option[ImageCatalog] = catalogsById.get(id)

  /**
    * Returns a catalog appropriate for a given wavelength
    * TODO Re-enable catalog selection an a per-wavelength basis when handling
    * of 2MASS catalogs has been improved
    */
  def catalogForWavelength(w: Option[Wavelength]): ImageCatalog = DssGemini
    /* As per user request, don't delete this code
    w match {
      case Some(d) if d <= DssCutoff                           => DssGemini
      case Some(d) if d <= MassJCutoff                         => TwoMassJ
      case Some(d) if d <= MassHCutoff                         => TwoMassH
      case Some(_)                                             => TwoMassK
      case None                                                => DefaultImageCatalog
    }
    */
}

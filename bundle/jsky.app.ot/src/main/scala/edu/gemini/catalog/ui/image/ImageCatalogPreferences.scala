package edu.gemini.catalog.ui.image

import java.io.File
import java.nio.file.Path

import edu.gemini.catalog.image.ImageCatalog
import jsky.util.Preferences
import squants.information.Information
import squants.information.InformationConversions._

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

/**
  * Preferences handling the image catalog, like cache size, etc
  * This are backed up to disk with the other jsky properties
  */
case class ImageCatalogPreferences(cacheDir: Path, imageCacheSize: Information, defaultCatalog: ImageCatalog)

object ImageCatalogPreferences {
  val DefaultCacheSize: Information = 500.mb

  private val ImageDefaultCatalog = "ot.catalog.default"
  private val ImageMaxCacheSize = "ot.cache.size"

  private def zero: ImageCatalogPreferences = {
    val tmpDir = new File(System.getProperty("java.io.tmpdir")).toPath
    ImageCatalogPreferences(tmpDir, DefaultCacheSize, ImageCatalog.DefaultImageCatalog)
  }

  /**
    * Indicates the user preferences about Image Catalogs
    */
  def preferences(): Task[ImageCatalogPreferences] = Task.delay {
    \/.fromTryCatchNonFatal {
      val cachePath = Preferences.getPreferences.getCacheDir.toPath
      // Try to parse preferences, Preferences.get reads a file
      val size = Option(Preferences.get(ImageMaxCacheSize)).map(_.toDouble)
      val catalog = ImageCatalog.all.find(_.id.filePrefix === Preferences.get(ImageDefaultCatalog)).getOrElse(ImageCatalog.DefaultImageCatalog)

      ImageCatalogPreferences(cachePath, size.map(_.megabytes).getOrElse(ImageCatalogPreferences.DefaultCacheSize), catalog)
    }.getOrElse(zero)
  }

  /**
    * Sets the user preferences about Image Catalogs
    */
  def preferences(prefs: ImageCatalogPreferences): Task[Unit] = Task.delay {
    // These calls write to a file
    Preferences.set(ImageMaxCacheSize, prefs.imageCacheSize.toMegabytes.toString)
    Preferences.set(ImageDefaultCatalog, prefs.defaultCatalog.id.filePrefix)
  }
}

package jsky.app.ot.userprefs.images

import edu.gemini.shared.util.immutable.{Option => JOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.pio.{ParamSet, Pio, PioFactory}
import jsky.app.ot.userprefs.model.{ExternalizablePreferences, PreferencesSupport}
import squants.information.Information
import squants.information.InformationConversions._

import scalaz._
import Scalaz._

case class ImageCatalogPreferences(imageCacheSize: Information) extends ExternalizablePreferences {
  override def toParamSet(factory: PioFactory): ParamSet = {
    factory.createParamSet(ImageCatalogPreferences.IMAGE_CATALOG_PREFS) <| {Pio.addDoubleParam(factory, _, ImageCatalogPreferences.IMAGE_CACHE_SIZE, imageCacheSize.toMegabytes)}
  }
}

object ImageCatalogPreferences {
  val IMAGE_CATALOG_PREFS = "imageCatalog"
  val IMAGE_CACHE_SIZE = "cacheSize"
  val DefaultCacheSize = 500.mb

  val Factory = new ExternalizablePreferences.Factory[ImageCatalogPreferences] {
    override def create(container: JOption[ParamSet]): ImageCatalogPreferences = {
      container.asScalaOpt.map { pref =>
        val cacheSize = Pio.getDoubleValue(pref, IMAGE_CACHE_SIZE, DefaultCacheSize.toMegabytes)
        ImageCatalogPreferences(cacheSize.megabytes)
      } | ImageCatalogPreferences(DefaultCacheSize)
    }
  }

  private val support = new PreferencesSupport[ImageCatalogPreferences](IMAGE_CATALOG_PREFS, Factory)

  def get = support.fetch()

  def set(size: Information) = support.store(ImageCatalogPreferences(size))
}

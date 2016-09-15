package jsky.app.ot.userprefs.images

import java.awt.{Component => JComponent}
import javax.swing.Icon

import edu.gemini.catalog.image.{ImageCatalog, ImageCatalogPreferences}
import edu.gemini.shared.gui.textComponent.NumberField
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption, Some => JSome}
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.app.ot.userprefs.ui.{PreferenceDialog, PreferencePanel}
import jsky.util.gui.DialogUtil

import scalaz._
import Scalaz._
import squants.information.InformationConversions._

import scala.swing.{Button, Component, Label}
import scala.swing.event.{ButtonClicked, ValueChanged}
import scalaz.concurrent.Task

class ImageCatalogPreferencesPanel extends PreferencePanel {
  val CacheSizeDescription = "Change the max allowed size of the cache and/or clear"

  val initialValue = ImageCatalog.preferences().unsafePerformSync
  val cacheTxt = PreferenceDialog.mkNote(CacheSizeDescription)
  val clearCacheButton = new Button("Clear cache") {
    reactions += {
      case ButtonClicked(_) =>
        Task.fork(ImageCatalog.clearCache).unsafePerformAsync {
          case \/-(_) => // Ignore
          case -\/(e) => DialogUtil.error(s"Error while cleaning the cache: ${e.getMessage}")
        }
    }
  }

  val cacheSizeField = new NumberField(initialValue.imageCacheSize.toMegabytes.some, false) {
    override def valid(d: Double): Boolean = d >= 0
    reactions += {
      case ValueChanged(_) if text.nonEmpty =>
        // this is guaranteed to be a positive double
        ImageCatalog.preferences(ImageCatalogPreferences(this.text.toDouble.megabytes, ImageCatalogPreferences.DefaultImageServer)).unsafePerformSync
    }
  }

  val component = new MigPanel(LC().insets(20, 10, 20, 10).fill()) {
      add(new Label("Cache size:"), CC())
      add(cacheSizeField, CC().alignX(LeftAlign))
      add(new Label("MB"), CC())
      add(clearCacheButton, CC())
      add(Component.wrap(cacheTxt), CC().span(4).growX().newline())
    }

  override def getDisplayName: String = "Image Catalog"

  override def getToolTip: JOption[String] = new JSome("Preferences for image catalogues")

  override def getIcon: JOption[Icon] = JNone.instance()

  override def getUserInterface: JComponent = component.peer
}

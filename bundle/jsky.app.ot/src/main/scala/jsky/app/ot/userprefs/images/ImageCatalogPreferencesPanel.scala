package jsky.app.ot.userprefs.images

import java.awt.{Component => JComponent}
import javax.swing.Icon

import edu.gemini.catalog.image.{ImageCacheOnDisk, ImageCatalog, ImageCatalogPreferences}
import edu.gemini.shared.gui.textComponent.{NumberField, TextRenderer}
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption, Some => JSome}
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.app.ot.userprefs.ui.{PreferenceDialog, PreferencePanel}
import jsky.util.gui.DialogUtil

import scalaz._
import Scalaz._
import squants.information.InformationConversions._

import scala.swing.{Button, ComboBox, Component, Label}
import scala.swing.event.{ButtonClicked, SelectionChanged, ValueChanged}
import scalaz.concurrent.Task

class ImageCatalogPreferencesPanel extends PreferencePanel {
  val CacheSizeDescription = "Change the max allowed size of the cache and/or clear"
  val DefaultCatalogDescription = "Select the default image catalog"

  val initialValue = ImageCatalogPreferences.preferences().unsafePerformSync

  lazy val catalogsComboBox = new ComboBox[ImageCatalog](ImageCatalog.all) with TextRenderer[ImageCatalog] {
    override def text(a: ImageCatalog): String = a.displayName

    listenTo(selection)
    selection.item = initialValue.defaultCatalog
    reactions += {
      case SelectionChanged(_) if cacheSizeField.text.nonEmpty =>
        ImageCatalogPreferences.preferences(ImageCatalogPreferences(cacheSizeField.text.toDouble.megabytes, selection.item)).unsafePerformSync
    }
  }

  lazy val catalogsTxt = PreferenceDialog.mkNote(DefaultCatalogDescription)

  lazy val cacheSizeField: NumberField = new NumberField(initialValue.imageCacheSize.toMegabytes.some, false) {
    override def valid(d: Double): Boolean = d >= 0

    reactions += {
      case ValueChanged(_) if text.nonEmpty =>
        // this is guaranteed to be a positive double
        ImageCatalogPreferences.preferences(ImageCatalogPreferences(this.text.toDouble.megabytes, catalogsComboBox.selection.item)).unsafePerformSync
    }
  }

  lazy val cacheTxt = PreferenceDialog.mkNote(CacheSizeDescription)

  lazy val clearCacheButton = new Button("Clear cache") {
    reactions += {
      case ButtonClicked(_) =>
        Task.fork(ImageCacheOnDisk.clearCache).unsafePerformAsync {
          case \/-(_) => // Ignore
          case -\/(e) => DialogUtil.error(s"Error while cleaning the cache: ${e.getMessage}")
        }
    }
  }

  lazy val component = new MigPanel(LC().insets(20, 10, 20, 10).fill()) {
      add(new Label("Default Image Catalog:"), CC())
      add(catalogsComboBox, CC().span(3).growX())
      add(Component.wrap(catalogsTxt), CC().span(4).growX().newline())
      add(new Label("Cache size:"), CC().growX().newline())
      add(cacheSizeField, CC())
      add(new Label("MB"), CC())
      add(clearCacheButton, CC().pushX().alignX(RightAlign))
      add(Component.wrap(cacheTxt), CC().span(4).growX().newline())
    }

  override def getDisplayName: String = "Image Catalog"

  override def getToolTip: JOption[String] = new JSome("Preferences for image catalogues")

  override def getIcon: JOption[Icon] = JNone.instance()

  override def getUserInterface: JComponent = component.peer
}

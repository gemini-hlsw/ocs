package jsky.app.ot.userprefs.images

import java.awt.{Component => JComponent}
import javax.swing.Icon

import edu.gemini.catalog.image.{ImageCatalog, ImageCatalogPreferences}
import edu.gemini.shared.gui.textComponent.NumberField
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption, Some => JSome}
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.app.ot.userprefs.ui.PreferencePanel

import scalaz._
import Scalaz._
import squants.information.InformationConversions._

import scala.swing.Label
import scala.swing.event.ValueChanged

class ImageCatalogPreferencesPanel extends PreferencePanel {

  val initialValue = ImageCatalog.preferences().unsafePerformSync

  val cacheSizeField = new NumberField(initialValue.imageCacheSize.toMegabytes.some, false) {
    override def valid(d: Double): Boolean = d >= 0
    reactions += {
      case ValueChanged(_) if text.nonEmpty =>
        // this is guaranteed to be a positive double
        ImageCatalog.preferences(ImageCatalogPreferences(this.text.toDouble.megabytes)).unsafePerformSync
    }
  }

  val component = new MigPanel(LC().insets(20, 10, 20, 10).fill()) {
      add(new Label("Cache size:"), CC())
      add(cacheSizeField, CC().alignX(LeftAlign).growX())
      add(new Label("MB"), CC().growX())
    }

  override def getDisplayName: String = "Image Catalog"

  override def getToolTip: JOption[String] = new JSome("Preferences for image catalogues")

  override def getIcon: JOption[Icon] = JNone.instance()

  override def getUserInterface: JComponent = component.peer
}

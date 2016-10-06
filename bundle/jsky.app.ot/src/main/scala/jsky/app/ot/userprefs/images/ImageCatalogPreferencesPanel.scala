package jsky.app.ot.userprefs.images

import java.awt.{Component => JComponent}
import java.text.DecimalFormat
import javax.swing.Icon

import edu.gemini.catalog.image.ImageCacheOnDisk
import edu.gemini.catalog.ui.image.ImageCatalogPreferences
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

/**
  * Preferences panel to give access to the images cache
  */
class ImageCatalogPreferencesPanel extends PreferencePanel {
  val CacheSizeDescription = "Change the max allowed size of the cache and/or clear"

  val initialValue = ImageCatalogPreferences.preferences().unsafePerformSync

  val df = new DecimalFormat <| {_.setParseIntegerOnly(true)} <| {_.setMinimumFractionDigits(0)} <| {_.setMinimumIntegerDigits(0)} <| {_.setGroupingUsed(false)}

  lazy val cacheSizeField: NumberField = new NumberField(initialValue.imageCacheSize.toMegabytes.some, false, df) {
    override def valid(d: Double): Boolean = d >= 0

    reactions += {
      case ValueChanged(_) if text.nonEmpty =>
        // this is guaranteed to be a positive double
        val task = for {
          prefs <- ImageCatalogPreferences.preferences()
          save  <- ImageCatalogPreferences.preferences(prefs.copy(imageCacheSize = this.text.toDouble.megabytes))
        } yield save
        task.unsafePerformSync
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

  lazy val component = new MigPanel(LC().insets(10, 10, 20, 10).fill()) {
      add(new Label("Cache size:"), CC().alignX(LeftAlign).newline())
      add(cacheSizeField, CC().growX().pushX())
      add(new Label("MB"), CC())
      add(clearCacheButton, CC().alignX(RightAlign))
      add(Component.wrap(cacheTxt), CC().span(4).growX().newline())
    }

  override def getDisplayName: String = "Image Catalog"

  override def getToolTip: JOption[String] = new JSome("Preferences for image catalogues")

  override def getIcon: JOption[Icon] = JNone.instance()

  override def getUserInterface: JComponent = component.peer
}

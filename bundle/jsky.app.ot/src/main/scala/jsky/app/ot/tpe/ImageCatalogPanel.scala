package jsky.app.ot.tpe

import java.time.Instant
import javax.swing._

import scalaz._
import Scalaz._
import edu.gemini.catalog.image._
import edu.gemini.catalog.ui.image.{ObsWavelengthExtractor, ObservationCatalogOverrides}
import edu.gemini.catalog.ui.tpe.CatalogImageDisplay
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.app.ot.userprefs.images.ImageCatalogPreferencesPanel
import jsky.util.gui.Resources

import scala.swing.event.ButtonClicked
import scala.swing.{Button, Component, Dialog, Label, RadioButton, Swing}
import scalaz.concurrent.Task

case class ImageLoadingFeedback() extends Label {
}

object ImageLoadingFeedback {
  val spinnerIcon: ImageIcon = Resources.getIcon("spinner16.gif")
  val warningIcon: ImageIcon = Resources.getIcon("eclipse/alert.gif")
  val errorIcon: ImageIcon   = Resources.getIcon("error_tsk.gif")
}

/**
  * Panel of radio buttons of Image catalogs offered by the TPE.
  */
final class ImageCatalogPanel(imageDisplay: CatalogImageDisplay) {
  private lazy val buttonGroup = new ButtonGroup
  private lazy val buttons =  ImageCatalog.all.map(mkButton)
  private lazy val feedbackPerCatalog =  buttons.collect { case (c, _, f) => c -> f }.toMap
  private lazy val toolsButton = new Button("") {
    tooltip = "Preferences..."
    icon = new ImageIcon(getClass.getResource("/resources/images/eclipse/engineering.gif"))

    reactions += {
      case ButtonClicked(_) =>
        new Dialog() {
          val closeButton = new Button("Close") {
            reactions += {
              case ButtonClicked(_) =>
                close()

                // Reset the image if needed
                val r = for {
                  p <- ImageCatalogPreferences.preferences() // At this moment the default catalog may have changed
                } yield {
                  val f = buttons.find(_._1 === p.defaultCatalog).map(_._3)
                  f match {
                    case Some(x) if selectedCatalog.forall(_ =/= p.defaultCatalog) => imageDisplay.loadSkyImage(listenerFor(x)) // Reload if the selected catalog is not the default
                    case _                                                         => ()
                  }
                }
                r.unsafePerformSync

                // Reset the selection
                TpeContext.fromTpeManager.foreach(t => resetCatalogue(t.obsShell))
            }
          }

          contents = new MigPanel(LC().fill().insets(5)) {
            add(new ImageCatalogPreferencesPanel().component, CC())
            add(closeButton, CC().alignX(RightAlign).newline())
          }
          defaultButton = closeButton
          modal = true
          setLocationRelativeTo(this)
        }.open()
    }
  }

  lazy val panel: Component = new MigPanel(LC().fill().insets(0.px)) {
    add(new Label("Image Catalog:"), CC())
    add(toolsButton, CC())
    buttons.foreach { case (c, b, f) =>
      add(b, CC().newline())
      add(f, CC())
      buttonGroup.add(b.peer)
      b.reactions += {
        case ButtonClicked(_) =>
          // Read the current key on the tpe
          val key = TpeContext.fromTpeManager.flatMap(_.obsKey)

          // Update the image and store the override
          key.foreach { k =>
            val actions = for {
              _ <- ObservationCatalogOverrides.storeOverride(k, c)
              _ <- Task.delay(imageDisplay.loadSkyImage(listenerFor(f)))
            } yield ()
            actions.unsafePerformSync
          }
        }
    }
  }

  def listenerFor(f: ImageLoadingFeedback) = new ImageLoadingListener {
    override def downloadStarts(): Unit = Swing.onEDT {
      f.icon = ImageLoadingFeedback.spinnerIcon
      f.tooltip = "Downloading..."
    }

    override def downloadCompletes(): Unit = Swing.onEDT {
      f.icon = null
      f.tooltip = ""
    }

    override def downloadError(): Unit = Swing.onEDT {
      f.icon = ImageLoadingFeedback.errorIcon
      f.tooltip = "Error when downloading"
    }
  }

  private def updateSelection(catalog: ImageCatalog): Unit =
    buttons.find(_._1 === catalog).foreach { _._2.selected = true}

  private def selectedCatalog: Option[ImageCatalog] =
    buttons.find(_._2.selected).map(_._1)

  private def showAsLoading(catalogues: CataloguesInUse): Unit = {
    val cataloguesInProgress = feedbackPerCatalog.filter(u => catalogues.inProgress.contains(u._1))
    val cataloguesInError = feedbackPerCatalog.filter(u => catalogues.failed.contains(u._1))
    val cataloguesIdle = feedbackPerCatalog.filterNot(u => cataloguesInError.contains(u._1) || cataloguesInProgress.contains(u._1))
    cataloguesInProgress.foreach { _._2.icon = ImageLoadingFeedback.spinnerIcon }
    cataloguesInError.foreach { _._2.icon = ImageLoadingFeedback.errorIcon }
    cataloguesIdle.foreach { _._2.icon = null }
  }

  /**
    * Updates the UI to reflect the state of downloading images
    *
    * Must be called from the EDT
    */
  def resetCatalogProgressState(): Unit = {
    // Verify we are on the EDT. We don't want to use Swing.onEDT inside
    assert(SwingUtilities.isEventDispatchThread)
    val tpeManager = TpeContext.fromTpeManager

    val catalogButtonsUpdate = for {
        tpe    <- tpeManager
        ctx    <- tpe.obsContext
        base   <- tpe.targets.base
        when   = ctx.getSchedulingBlockStart.asScalaOpt | Instant.now.toEpochMilli
        coords <- base.getTarget.coords(when)
      } yield ImagesInProgress.cataloguesInUse(coords).map(showAsLoading)

    catalogButtonsUpdate.sequenceU.unsafePerformSync
  }

  def resetCatalogue(observation: Option[ISPObservation]): Unit = {
    // Verify we are on the EDT. We don't want to use Swing.onEDT
    assert(SwingUtilities.isEventDispatchThread)

    val tpeManager = TpeContext.fromTpeManager

    val wavelength = tpeManager.flatMap(ObsWavelengthExtractor.extractObsWavelength)

    val selectedCatalog = observation.map(_.getNodeKey)
      .fold(ImageCatalogPreferences.preferences().map(_.defaultCatalog))(ObservationCatalogOverrides.catalogFor(_, wavelength))

    selectedCatalog.map(updateSelection).unsafePerformSync

    resetCatalogProgressState()
  }

  private def mkButton(c: ImageCatalog): (ImageCatalog, RadioButton, ImageLoadingFeedback) =
    (c, new RadioButton(s"${c.shortName}") <| {_.tooltip = c.displayName} , new ImageLoadingFeedback())
}

package jsky.app.ot.tpe

import java.time.Instant
import javax.swing._

import scalaz._
import Scalaz._
import edu.gemini.catalog.image._
import edu.gemini.catalog.ui.image.{ImageLoadingListener, ObsWavelengthExtractor, ObservationCatalogOverrides}
import edu.gemini.catalog.ui.tpe.CatalogImageDisplay
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.app.ot.userprefs.images.ImageCatalogPreferencesPanel
import jsky.util.gui.Resources

import scala.swing.event.{ButtonClicked, MouseClicked}
import scala.swing.{Button, Component, Dialog, Label, RadioButton, Swing}
import scalaz.concurrent.Task

object ImageCatalogPanel {
  /**
    * Tries to reset the catalog panel state if open
    */
  private def resetCatalogPanel: Task[Unit] = Task.delay {
    for {
      tpe <- Option(TpeManager.get())
      ctx <- TpeContext.fromTpeManager
      obs <- ctx.obsShell
    } {
      tpe.getTpeToolBar.updateImageCatalogState(obs)
    }
  }

  def resetListener: ImageLoadingListener[Unit] =
    ImageLoadingListener(
      Task.delay(Swing.onEDT(ImageCatalogPanel.resetCatalogPanel.unsafePerformSync)),
      Task.delay(Swing.onEDT(ImageCatalogPanel.resetCatalogPanel.unsafePerformSync)),
      Task.delay(Swing.onEDT(ImageCatalogPanel.resetCatalogPanel.unsafePerformSync)))

  def isCatalogSelected(catalog: ImageCatalog): Boolean =
    Option(TpeManager.get()).exists(_.getTpeToolBar.isCatalogSelected(catalog))
}

/**
  * Panel of radio buttons of Image catalogs offered by the TPE.
  */
final class ImageCatalogPanel(imageDisplay: CatalogImageDisplay) {

  case class ImageLoadingFeedback(catalog: ImageCatalog) extends Label {

    def markDownloading(): Unit = {
      icon = ImageLoadingFeedback.spinnerIcon
      tooltip = "Downloading..."
      deafTo(mouse.clicks)
    }

    def markIdle(): Unit = {
      icon = null
      tooltip = ""
      deafTo(mouse.clicks)
    }

    def markError(): Unit = {
      icon = ImageLoadingFeedback.errorIcon
      tooltip = "Error when downloading"
      listenTo(mouse.clicks)
      reactions += {
        case MouseClicked(_, _, _, _, _) =>
          requestImage(catalog)
      }
    }
  }

  case class CatalogRow(button: RadioButton, feedback: ImageLoadingFeedback)

  object ImageLoadingFeedback {
    val spinnerIcon: ImageIcon = Resources.getIcon("spinner16.gif")
    val warningIcon: ImageIcon = Resources.getIcon("eclipse/alert.gif")
    val errorIcon: ImageIcon   = Resources.getIcon("error_tsk.gif")
  }

  private lazy val buttonGroup = new ButtonGroup
  private lazy val catalogRows = ImageCatalog.all.map(mkRow)
  private lazy val toolsButton = new Button("") {
    tooltip = "Preferences..."
    icon = new ImageIcon(getClass.getResource("/resources/images/eclipse/engineering.gif"))

    reactions += {
      case ButtonClicked(_) =>
        new Dialog() {
          title = "Image Catalog Preferences"
          Resources.setOTFrameIcon(this.peer)

          val closeButton = new Button("Close") {
            reactions += {
              case ButtonClicked(_) =>
                close()
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

  lazy val panel: Component = new MigPanel(LC().fill().insets(0.px).gridGap(0.px, 0.px)) {
    add(new Label("Image Catalog:"), CC())
    add(toolsButton, CC().alignX(RightAlign))
    catalogRows.foreach { row =>
      add(row.button, CC().newline())
      add(row.feedback, CC().alignX(RightAlign))
      buttonGroup.add(row.button.peer)
      row.button.reactions += {
        case ButtonClicked(_) =>
          requestImage(row.feedback.catalog)
        }
    }
  }

  private def requestImage(catalog: ImageCatalog) =
    // Read the current key and wavelength on the tpe
    for {
        tpe <- TpeContext.fromTpeManager
        key <- tpe.obsKey
        wv  =   ObsWavelengthExtractor.extractObsWavelength(tpe)
      } {
        // Update the image and store the override
        val actions =
          for {
            _ <- ObservationCatalogOverrides.storeOverride(key, catalog, wv)
            _ <- Task.delay(imageDisplay.loadSkyImage())
          } yield ()
        actions.unsafePerformSync
      }

  private def updateSelection(catalog: ImageCatalog): Unit =
    catalogRows.find(_.feedback.catalog === catalog).foreach { _.button.selected = true }

  private def selectedCatalog: Option[ImageCatalog] =
    catalogRows.find(_.button.selected).map(_.feedback.catalog)

  private def showAsLoading(catalogues: CataloguesInUse): Unit = {
    val cataloguesInProgress = catalogRows.filter(r => catalogues.inProgress.contains(r.feedback.catalog))
    val cataloguesInError = catalogRows.filter(r => catalogues.failed.contains(r.feedback.catalog))
    val cataloguesIdle = catalogRows.filterNot(u => cataloguesInError.contains(u) || cataloguesInProgress.contains(u))
    cataloguesInProgress.foreach(_.feedback.markDownloading())
    cataloguesInError.foreach(_.feedback.markError())
    cataloguesIdle.foreach(_.feedback.markIdle())
  }

  /**
    * Updates the UI to reflect the state of downloading images
    *
    * Must be called from the EDT
    */
  private def resetCatalogProgressState: Task[Option[Unit]] = {
    // Verify we are on the EDT. We don't want to use Swing.onEDT inside
    assert(SwingUtilities.isEventDispatchThread)
    val tpeContext = TpeContext.fromTpeManager

    val catalogButtonsUpdate = for {
        tpe    <- tpeContext
        ctx    <- tpe.obsContext
        ast    <- tpe.targets.asterism
        when   = ctx.getSchedulingBlockStart.asScalaOpt.map(a => Instant.ofEpochMilli(a.toLong))
        coords <- ast.basePosition(when orElse Some(Instant.now))
       } yield KnownImagesSets.cataloguesInUse(coords).map(showAsLoading)

    catalogButtonsUpdate.sequenceU
  }

  def resetCatalogue(observation: ISPObservation): Unit = {
    // Verify we are on the EDT. We don't want to use Swing.onEDT
    assert(SwingUtilities.isEventDispatchThread)

    val wavelength = TpeContext.fromTpeManager.flatMap(ObsWavelengthExtractor.extractObsWavelength)

    val updateSelectedCatalog = ObservationCatalogOverrides.catalogFor(observation.getNodeKey, wavelength).map(updateSelection)
    // run both side effects synchronously inside EDT
    Nondeterminism[Task].both(updateSelectedCatalog, resetCatalogProgressState).unsafePerformSync
  }

  def isCatalogSelected(catalog: ImageCatalog): Boolean = {
    catalogRows.find(_.button.selected).exists(_.feedback.catalog === catalog)
  }

  private def mkRow(c: ImageCatalog): CatalogRow =
    CatalogRow(new RadioButton(s"${c.shortName}") <| {_.tooltip = c.displayName} , new ImageLoadingFeedback(c))
}

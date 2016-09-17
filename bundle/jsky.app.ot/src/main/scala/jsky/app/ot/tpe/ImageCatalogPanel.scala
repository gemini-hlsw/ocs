package jsky.app.ot.tpe

import javax.swing._

import scalaz._
import Scalaz._
import edu.gemini.catalog.image.{ImageCatalog, ImageLoadingListener}
import edu.gemini.catalog.ui.image.ObservationCatalogOverrides
import edu.gemini.catalog.ui.tpe.CatalogImageDisplay
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.ui.miglayout.MigPanel
import edu.gemini.ui.miglayout.constraints._
import jsky.app.ot.userprefs.images.ImageCatalogPreferencesPanel
import jsky.util.gui.Resources

import scala.swing.event.ButtonClicked
import scala.swing.{Button, Dialog, Label, RadioButton, Swing}
import scalaz.concurrent.Task

case class ImageLoadingFeedback() extends Label {
}

object ImageLoadingFeedback {
  val spinnerIcon = Resources.getIcon("spinner16.gif")
  val warningIcon = Resources.getIcon("eclipse/alert.gif")
  val errorIcon   = Resources.getIcon("error_tsk.gif")
}

/**
  * Panel of radio buttons of Image catalogs offered by the TPE.
  */
final class ImageCatalogPanel(imageDisplay: CatalogImageDisplay) {
  lazy val buttonGroup = new ButtonGroup
  lazy val buttons =  ImageCatalog.all.map(mkButton)
  lazy val toolsButton = new Button("") {
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
                  p <- ImageCatalog.preferences() // At this moment the default catalog may have changed
                } yield {
                  val f = buttons.find(_._1 === p.defaultCatalog).map(_._3)
                  f match {
                    case Some(x) if selectedCatalog.forall(_ =/= p.defaultCatalog) => imageDisplay.loadSkyImage(listenerFor(x)) // Reload if the selected catalog is not the default
                    case _                                                         => ()
                  }
                }
                r.unsafePerformSync

                // Reset the selection
                for {
                  tpe <- Option(TpeManager.get())
                  iw  <- Option(tpe.getImageWidget)
                  c   <- Option(iw.getContext)
                } yield resetCatalogue(c.obsShell)
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

  lazy val panel = new MigPanel(LC().fill().insets(0.px)) {
    add(new Label("Image Catalog:"), CC())
    add(toolsButton, CC())
    buttons.foreach { case (c, b, f) =>
      add(b, CC().newline())
      add(f, CC())
      buttonGroup.add(b.peer)
      b.reactions += {
        case ButtonClicked(_) =>
          // Read the current key on the tpe
          val key = for {
            tpe <- Option(TpeManager.get())
            iw <- Option(tpe.getImageWidget)
            c <- Option(iw.getContext)
            k <- c.obsKey
          } yield k

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

  def resetCatalogue(observation: Option[ISPObservation]): Unit = {
    // Verify we are on the EDT. We don't want to use Swing.onEDT
    assert(SwingUtilities.isEventDispatchThread)

    val catalogue = observation.map(_.getNodeKey)
      .fold(ImageCatalog.preferences().map(_.defaultCatalog))(ObservationCatalogOverrides.catalogFor)

    catalogue.map(updateSelection).unsafePerformSync
  }

  private def mkButton(c: ImageCatalog): (ImageCatalog, RadioButton, ImageLoadingFeedback) =
    (c, new RadioButton(s"${c.shortName}") <| {_.tooltip = c.displayName} , new ImageLoadingFeedback())
}

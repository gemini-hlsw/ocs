package jsky.app.ot.tpe

import java.awt.event.{ActionEvent, ActionListener}
import javax.swing._

import scalaz._
import Scalaz._
import edu.gemini.catalog.image.ImageCatalog
import edu.gemini.catalog.ui.image.ObservationCatalogOverrides
import edu.gemini.catalog.ui.tpe.CatalogImageDisplay
import edu.gemini.pot.sp.ISPObservation

import scalaz.concurrent.Task

/**
  * Panel of radio buttons of Image catalogs offered by the TPE.
  */
final class ImageCatalogPanel(imageDisplay: CatalogImageDisplay) {
  val panel = new JPanel
  val label = new JLabel("Image Catalog")
  val buttonGroup = new ButtonGroup
  panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS))

  val buttons =  ImageCatalog.all.map(mkButton)
  panel.add(label)
  buttons.foreach { case (c, b) =>
    panel.add(b)
    buttonGroup.add(b)
    b.addActionListener(new ActionListener() {
      override def actionPerformed(e: ActionEvent): Unit = {
        // Read the current key on the tpe
        val key = for {
            tpe <- Option(TpeManager.get())
            iw  <- Option(tpe.getImageWidget)
            c   <- Option(iw.getContext)
            o   <- c.obsShell
          } yield o.getNodeKey

        // Update the image and store the override
        key.foreach { k =>
          val actions = for {
            _ <- ObservationCatalogOverrides.storeOverride(k, c)
            _ <- Task.delay(imageDisplay.loadSkyImage())
          } yield ()
          actions.unsafePerformSync
        }
      }
    })
  }

  private def updateSelection(catalog: ImageCatalog): Unit = {
    buttons.find(_._1 === catalog).foreach { _._2.setSelected(true)}
  }

  def resetCatalogue(catalogue: Option[ISPObservation]): Unit = {
    // Verify we are on the EDT. We don't want to use Swing.onEDT
    assert(SwingUtilities.isEventDispatchThread)
    catalogue.map(_.getNodeKey).foreach { key =>
      val actions = for {
        c <- ObservationCatalogOverrides.catalogFor(key)
        _ <- Task.delay(updateSelection(c))
      } yield ()
      actions.unsafePerformSync
    }
  }

  private def mkButton(c: ImageCatalog): (ImageCatalog, JRadioButton) =
    (c, new JRadioButton(s"${c.shortName}") <| {_.setToolTipText(c.displayName)} <| {_.setSelected(c === ImageCatalog.user)})
}

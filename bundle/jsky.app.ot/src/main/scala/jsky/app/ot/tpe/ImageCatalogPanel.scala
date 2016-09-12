package jsky.app.ot.tpe

import java.awt.event.{ActionEvent, ActionListener}
import javax.swing._

import scalaz._
import Scalaz._
import edu.gemini.catalog.image.ImageCatalog
import edu.gemini.catalog.ui.tpe.CatalogImageDisplay

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
        ImageCatalog.user(c)
        imageDisplay.loadSkyImage()
      }
    })
  }

  private def mkButton(c: ImageCatalog): (ImageCatalog, JRadioButton) =
    (c, new JRadioButton(s"${c.shortName}") <| {_.setToolTipText(c.displayName)} <| {_.setSelected(c === ImageCatalog.user)})
}

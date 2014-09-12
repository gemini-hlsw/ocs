package jsky.app.ot.gemini.editor.targetComponent

import java.awt.geom.{Rectangle2D, Arc2D}

import edu.gemini.ags.api.AgsGuideQuality
import edu.gemini.spModel.rich.shared.immutable.asScalaOpt
import jsky.app.ot.util.Rendering

import java.awt.{BasicStroke, Color}
import java.awt.image.BufferedImage
import javax.swing.ImageIcon

/**
 * Calculates icons to be used for the various AgsGuideQuality values, with
 * enabled and disabled variations.
 */
object GuidingIcon {
  import AgsGuideQuality.All

  private val darkGreen = new Color(0, 153, 0)

  private val icons = (for {
    x <- None :: All.map(q => Some(q))
    y <- List(true, false)
  } yield (x,y) -> makeIcon(x, y)).toMap

  def apply(q: Option[AgsGuideQuality], enabled: Boolean): ImageIcon =
    icons((q, enabled))

  // For use from Java with the Gemini Java Option ...
  def apply(q: edu.gemini.shared.util.immutable.Option[AgsGuideQuality], enabled: Boolean): ImageIcon =
    apply(q.asScalaOpt, enabled)

  // Create a Harvey Ball programmatically to represent the quality of the target.
  // See: http://en.wikipedia.org/wiki/Harvey_Balls
  private def makeIcon(q: Option[AgsGuideQuality], enabled: Boolean): ImageIcon = {
    new ImageIcon({
      val img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
      val g2  = img.createGraphics()

      Rendering.withQuality(g2) {
        // The bounding rectangle for the arcs.
        val r2  = new Rectangle2D.Double(2, 2, img.getWidth-4, img.getHeight-4)

        // Paint the inner component indicating quality.
        if (q.isDefined) {
          g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
          g2.setPaint(if (enabled) darkGreen else Color.gray)
          g2.fill(new Arc2D.Double(r2, 90, -90 * (4 - q.get.ord), Arc2D.PIE))
        }

        // Paint the outer border.
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
        g2.setPaint((enabled, q.isEmpty) match {
          case (true,  true)  => Color.red
          case (true,  false) => Color.black
          case (false, _)     => Color.gray
        })
        g2.draw(new Arc2D.Double(r2, 0, 360, Arc2D.OPEN))
      }

      img
    })
  }
}

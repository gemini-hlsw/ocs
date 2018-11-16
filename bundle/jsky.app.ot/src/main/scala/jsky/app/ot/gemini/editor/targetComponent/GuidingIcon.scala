package jsky.app.ot.gemini.editor.targetComponent

import edu.gemini.ags.api.AgsGuideQuality._
import java.awt.geom.{Arc2D, Rectangle2D}

import edu.gemini.ags.api.AgsGuideQuality
import jsky.app.ot.util.Rendering
import java.awt.{BasicStroke, Color}
import java.awt.image.BufferedImage

import javax.swing.ImageIcon

import scalaz._
import Scalaz._

/**
 * Calculates icons to be used for the various AgsGuideQuality values, with
 * enabled and disabled variations.
 */
object GuidingIcon {
  val sideLength = 16

  private val darkGreen  = new Color(  0, 153, 0)
  private val darkOrange = new Color(255, 147, 0)
  private val darkRed    = new Color(221,  38, 0)

  private val icons: Map[(AgsGuideQuality, Boolean), ImageIcon] = (for {
    q <- AgsGuideQuality.All
    e <- List(true, false)
  } yield (q, e) -> makeIcon(q, e)).toMap

  def apply(quality: AgsGuideQuality, enabled: Boolean): ImageIcon =
    icons((quality, enabled))

  // Create a Harvey Ball programmatically to represent the quality of the target.
  // See: http://en.wikipedia.org/wiki/Harvey_Balls
  private def makeIcon(q: AgsGuideQuality, enabled: Boolean): ImageIcon = {
    new ImageIcon({
      val img = new BufferedImage(sideLength, sideLength, BufferedImage.TYPE_INT_ARGB)
      val g2  = img.createGraphics()

      Rendering.withQuality(g2) {
        // The bounding rectangle for the arcs.
        val r2  = new Rectangle2D.Double(2, 2, img.getWidth - 4, img.getHeight - 4)

        // How many pieces of the pie chart to paint.
        val quarters: Int = q match {
          case DeliversRequestedIq   => 4
          case PossibleIqDegradation => 3
          case IqDegradation         => 2
          case PossiblyUnusable      => 1
          case Unusable              => 0
        }

        // Color of the pie chart pieces.
        val fillColor: Color = q match {
          case DeliversRequestedIq   => darkGreen
          case PossibleIqDegradation => darkGreen
          case IqDegradation         => darkOrange
          case PossiblyUnusable      => darkOrange
          case Unusable              => darkRed
        }

        // Color of the outline of the pie chart.
        val borderColor: Color = if (q === Unusable) darkRed else Color.black

        // Paint a white interior.
        g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
        g2.setPaint(Color.white)
        g2.fill(new Arc2D.Double(r2, 0, 360, Arc2D.PIE))

        // Paint the color-coded pie chart pieces.
        g2.setPaint(if (enabled) fillColor else Color.gray)
        g2.fill(new Arc2D.Double(r2, 90, -90 * quarters, Arc2D.PIE))

        // Paint the outer border.
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
        g2.setPaint(if (enabled) borderColor else Color.gray)
        g2.draw(new Arc2D.Double(r2, 0, 360, Arc2D.OPEN))
      }

      img
    })
  }
}

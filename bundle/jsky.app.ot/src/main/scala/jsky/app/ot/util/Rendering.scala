package jsky.app.ot.util

import java.awt.Graphics2D
import java.awt.RenderingHints._
import scala.collection.JavaConverters._

/**
 * Help with Java 2D rendering.
 */
object Rendering {
  val QualityHints = Map(
    KEY_ANTIALIASING -> VALUE_ANTIALIAS_ON,
    KEY_RENDERING -> VALUE_RENDER_QUALITY
  )

  def withQuality(g2d: Graphics2D)(draw: => Unit): Unit = {
    val oldHints = g2d.getRenderingHints
    try {
      g2d.setRenderingHints(QualityHints.asJava)
      draw
    } finally {
      g2d.setRenderingHints(oldHints)
    }
  }
}

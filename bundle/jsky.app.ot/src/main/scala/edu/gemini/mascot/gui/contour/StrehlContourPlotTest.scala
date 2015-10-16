package edu.gemini.mascot.gui.contour

import java.awt.{Graphics2D, Graphics, Dimension, Component}
import java.awt.geom.AffineTransform
import edu.gemini.ags.gems.mascot.{Strehl, Mascot, Star}
import javax.swing.JOptionPane

import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._

/**
 * Interactive test
 */
object StrehlContourPlotTest {
  def star(centerX: Double, centerY: Double,
                 bmag: Double, vmag: Double,
                 rmag: Double, jmag: Double,
                 hmag: Double, kmag: Double,
                 ra: Double, dec: Double): Star = {
    val coords = Coordinates(RightAscension.fromDegrees(ra), Declination.fromAngle(Angle.fromDegrees(dec)).getOrElse(Declination.zero))
    val magnitudes = List(new Magnitude(bmag, MagnitudeBand.B), new Magnitude(vmag, MagnitudeBand.V), new Magnitude(rmag, MagnitudeBand.R), new Magnitude(jmag, MagnitudeBand.J), new Magnitude(hmag, MagnitudeBand.H), new Magnitude(kmag, MagnitudeBand.K))
    val target = SiderealTarget("name", coords, magnitudes)
    Star.makeStar(target, centerX, centerY).copy(x = centerX, y = centerY)
  }

  val allStarList = List(
    star(1.25168, 0.801961, 11.34, 9.08, 11.09, 12.769, 11.977, 11.298, 49.9505, 41.5119),
    star(2.72678, 1.63212, 12.67, -27, 11.59, -27, -27, -27, 49.95, 41.5121),
    star(-32.9534, 43.4231, 14.19, 13.55, 12.63, 11.678, 11.088, 10.979, 49.9632, 41.5238),
    star(42.4108, 15.864, 14.71, 14.06, 13.11, 12.677, 12.144, 12.063, 49.9352, 41.5161),
    star(16.3359, 63.2018, 17.52, 17.25, 15.25, 15.621, 15.054, 14.551, 49.9449, 41.5292),
    star(-60.5757, 10.5022, 16.12, 15.54, 15.39, 14.666, 14.211, 14.21, 49.9734, 41.5146),
    star(-36.3314, 50.2519, 18.66, -27, 15.52, 15.287, 14.712, 14.452, 49.9645, 41.5256),
    star(-12.1038, 68.442, 18.35, 17.9, 18.02, 16.042, 15.803, 15.201, 49.9555, 41.5307),
    star(-25.6909, 66.7921, 19.68, -27, 18.03, 16.236, 15.846, 15.368, 49.9605, 41.5302),
    star(-16.2806, -33.0179, -27, -27, 18.56, -27, -27, -27, 49.957, 41.5025),
    star(-49.9113, -16.518, 19.69, -27, 18.87, -27, -27, -27, 49.9695, 41.5071),
    star(-48.2301, 1.932, -27, -27, 19.11, -27, -27, -27, 49.9689, 41.5122),
    star(-55.1361, -38.248, 20.32, -27, 19.22, -27, -27, -27, 49.9714, 41.5011),
    star(-39.1604, 56.062, -27, -27, 19.61, -27, -27, -27, 49.9655, 41.5273),
    star(42.5929, -34.8179, -27, 17.97, 19.72, 16.87, 16.199, 15.374, 49.9352, 41.502)
  )

  // Local class used to flip the image for comparison with the Yorick Mascot version,
  // which displays in the orientation of the FITS image
  class FlipyComponent(plot: ContourPlot) extends Component {
    setPreferredSize(new Dimension(plot.getWidth, plot.getHeight))

    override def paint(g: Graphics) {
        val g2d = g.asInstanceOf[Graphics2D]
        val trans = AffineTransform.getScaleInstance(1.0, -1.0)
        trans.translate(0.0, -plot.getHeight)
        g2d.drawRenderedImage(plot, trans)
    }
  }

  // Displays a message dialog containing a contour plot of the given Strehl object, for testing.
  def displayTest(s: Strehl) {
    val ret = StrehlContourPlot.create(s, 300)
    val c = new FlipyComponent(ret)
    JOptionPane.showMessageDialog(null, c)
  }

  def main(args: Array[String]) {
    val (_, strehlList) = Mascot.findBestAsterism(allStarList)

    var i = 0
    val n = strehlList.size
    for (s <- strehlList) {
      i += 1
      Mascot.defaultProgress(s, i, n)
      displayTest(s)
    }
  }
}

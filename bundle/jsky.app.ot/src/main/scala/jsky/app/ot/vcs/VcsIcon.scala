package jsky.app.ot.vcs

import jsky.app.ot.util.{OtColor, Rendering}
import java.awt._
import java.awt.geom.{Arc2D, Ellipse2D, Path2D}
import java.awt.image.BufferedImage
import javax.swing.ImageIcon

import jsky.util.gui.Resources

import scala.swing.Graphics2D

/**
 * Icons for all the VCS operations.
 */
object VcsIcon {
  private def load(name: String): ImageIcon = Resources.getIcon("vcs/vcs_%s.png".format(name))

  val Conflict = load("up_conflict")
  val ConflictPrev = load("conflict_prev")
  val ConflictNext = load("conflict_next")

  val Update = load("up")
  val Commit = load("ci")

  private val InSyncColor = OtColor.HONEY_DEW.darker
  private val NoPeerColor = OtColor.SKY.darker
  private val ErrorColor = OtColor.SALMON
  private val PendingSyncColor = OtColor.SALMON
  private val PendingUpdateColor = new Color(255, 175, 0)
  private val PendingCheckInColor = PendingUpdateColor

  private type Drawing = (Graphics2D, Int, Int) => Unit

  private val DownArrow: Drawing = (g2, w, h) => {
    g2.fill(new Polygon(Array(4, w - 4, w / 2), Array(h / 2 - 4, h / 2 - 4, h - 4), 3))
    //    g2.fill(new Polygon(Array(5, w-5, w/2), Array(h/2, h/2, h-4), 3))
    //    g2.fill(new Rectangle2D.Double(w/2-2,4,4,8))
  }

  private val UpArrow: Drawing = (g2, w, h) => {
    g2.fill(new Polygon(Array(4, w - 4, w / 2), Array(h / 2 + 4, h / 2 + 4, 4), 3))
    //    g2.fill(new Polygon(Array(5, w-5, w/2), Array(h/2, h/2, 4), 3))
    //    g2.fill(new Rectangle2D.Double(w/2-2,h/2,4,8))
  }

  private val CycleArrow: Drawing = (g2, w, h) => {
    g2.fill(new Polygon(Array(w / 2, w / 2 + 4, w / 2), Array(2, 7, 12), 3))
    g2.setStroke(new BasicStroke(2.0f))
    g2.draw(new Arc2D.Double(6, 6, w - 12, h - 12, 90.0, 270.0, Arc2D.OPEN))
  }

  private val Question: Drawing = (g2, w, h) => {
    g2.setFont(g2.getFont.deriveFont(22.0f).deriveFont(Font.BOLD))
    g2.drawString("?", 8.0f, h - 4.0f) // okay should be done w/ font metrics
  }

  private val Network: Drawing = (g2, w, h) => {
    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
    val path = new Path2D.Double()
    path.moveTo(6f, h - 6f) // lower left corner
    path.lineTo(6f, 6f) // upper left corner
    path.lineTo(w / 2 - 2f, h / 2 - 2f)
    path.moveTo(w / 2 + 2f, h / 2 + 2f)
    path.lineTo(w - 6f, h - 6f) // lower right corner
    path.lineTo(w - 6f, 6f) // upper right corner
    g2.draw(path)
  }

  private def makeIcon(d: Drawing, c: Color): ImageIcon = {
    val w = 24
    val h = 24

    def image: Image = {
      val img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
      val g2 = img.createGraphics

      Rendering.withQuality(g2) {
        val paint = new RadialGradientPaint(3f, 3f, w, Array(0.0f, 1.0f), Array(c.brighter(), c))
        g2.setPaint(paint)
        g2.fill(new Ellipse2D.Double(0, 0, w, h))
        g2.setPaint(Color.darkGray)
        d(g2, w, h)
      }
      g2.dispose()
      img
    }
    new ImageIcon(image)
  }

  val PendingSync = makeIcon(CycleArrow, PendingSyncColor)
  val PendingUpdate = makeIcon(DownArrow, PendingUpdateColor)
  val PendingCheckIn = makeIcon(UpArrow, PendingCheckInColor)
  val UpToDate = makeIcon(CycleArrow, InSyncColor)
  val NoPeer = makeIcon(Question, NoPeerColor)
  val BrokenLink = makeIcon(Network, ErrorColor)
}

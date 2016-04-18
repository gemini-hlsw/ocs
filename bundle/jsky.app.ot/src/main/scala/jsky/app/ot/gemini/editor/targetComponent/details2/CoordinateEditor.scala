package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.Color

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.core.{Declination, Angle, RightAscension, SiderealTarget, Coordinates, Target}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.TextBoxWidget

import scalaz._, Scalaz._

// RA and Dec
class CoordinateEditor extends TelescopePosEditor with ReentrancyHack {

  private[this] var spt = new SPTarget

  val raLens:  Target @?> RightAscension = Target.coords >=> Coordinates.ra .partial
  val decLens: Target @?> Declination    = Target.coords >=> Coordinates.dec.partial

  val ra, dec = new TextBoxWidget <| {w =>
    w.setColumns(10)
    w.setMinimumSize(w.getPreferredSize)
  }

  ra.addWatcher(watcher { s =>
    nonreentrant {
      Angle.parseHMS(clean(s)).map(RightAscension.fromAngle) match {
        case -\/(e) => ra.setForeground(Color.RED)
        case \/-(a) =>
          ra.setForeground(Color.BLACK)
          raLens.set(newTarget, a).foreach(spt.setTarget)
      }
    }
  })

  dec.addWatcher(watcher { s =>
    nonreentrant {
      Angle.parseDMS(clean(s)).map(Declination.fromAngle) match {
        case -\/(_) | \/-(None) => ra.setForeground(Color.RED)
        case \/-(Some(a)) =>
          dec.setForeground(Color.BLACK)
          decLens.set(newTarget, a).foreach(spt.setTarget)
      }
    }
  })

  def edit(ctx: GOption[ObsContext], target0: SPTarget, node: ISPNode): Unit = {
    spt = target0
    nonreentrant {
      Target.coords.get(newTarget).foreach { cs =>
        ra.setText(cs.ra.toAngle.formatHMS)
        ra.setForeground(Color.BLACK)
        dec.setText(cs.dec.formatDMS)
        dec.setForeground(Color.BLACK)
      }
    }
  }

  def newTarget: Target =
    spt.getTarget

  def clean(angle: String): String =
    angle.trim.replace(",", ".")

}

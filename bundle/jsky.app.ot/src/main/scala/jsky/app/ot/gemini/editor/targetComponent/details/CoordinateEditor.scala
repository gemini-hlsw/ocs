package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.CoordinateParam.Units
import edu.gemini.spModel.target.system.ITarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.TextBoxWidget

import scalaz.syntax.id._

// RA and Dec
class CoordinateEditor extends TelescopePosEditor with ReentrancyHack {

  private[this] var spt = new SPTarget

  val ra, dec = new TextBoxWidget <| {w =>
    w.setColumns(10)
    w.setMinimumSize(w.getPreferredSize)
  }

  ra.addWatcher(watcher { s =>
    nonreentrant {
      try {
        target.getRa.setValue(clean(s))
      } catch {
        case _: IllegalArgumentException => target.getRa.setAs(0, Units.DEGREES)
      }
      spt.notifyOfGenericUpdate()
    }
  })

  dec.addWatcher(watcher { s =>
    nonreentrant {
      clean(s) match {
        case "-" | "+" => // nop
        case s =>
          try {
            target.getDec.setValue(s)
          } catch {
            case _: IllegalArgumentException =>
              target.getDec.setAs(0, Units.DEGREES)
          }
          spt.notifyOfGenericUpdate()
      }
    }
  })

  def edit(ctx: GOption[ObsContext], target0: SPTarget, node: ISPNode): Unit = {
    spt = target0
    nonreentrant {
      ra.setText(target.getRa.toString)
      dec.setText(target.getDec.toString)
    }
  }

  def target: ITarget =
    spt.getTarget

  def clean(angle: String): String =
    angle.trim.replace(",", ".")

}

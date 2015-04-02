package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{Insets, GridBagConstraints, GridBagLayout}
import javax.swing.{JLabel, JPanel}

import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.CoordinateParam.Units
import edu.gemini.spModel.target.system.ITarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.TextBoxWidget

import scalaz.syntax.id._

// RA and Dec, side by side
class CoordinateEditor extends JPanel with TelescopePosEditor with ReentrancyHack {

  private[this] var spt = new SPTarget

  val ra, dec = new TextBoxWidget <| {w =>
    w.setColumns(10)
    w.setMinimumSize(w.getPreferredSize)
  }

  setLayout(new GridBagLayout)

  add(new JLabel("RA"), new GridBagConstraints <| { c =>
    c.gridx = 0
    c.insets = new Insets(0, 0, 0, 5)
    c.weighty = 0
  })

  add(ra, new GridBagConstraints <| { c =>
    c.gridx = 1
    c.insets = new Insets(0, 0, 0, 10)
    c.fill = GridBagConstraints.HORIZONTAL
    c.weightx = 2
  })

  add(new JLabel("Dec"), new GridBagConstraints <| { c =>
    c.gridx = 2
    c.insets = new Insets(0, 0, 0, 5)
    c.weighty = 0
  })

  add(dec, new GridBagConstraints <| { c =>
    c.gridx = 3
    c.fill = GridBagConstraints.HORIZONTAL
    c.weightx = 2
  })


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

  def edit(ctx: GOption[ObsContext], target0: SPTarget): Unit = {
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

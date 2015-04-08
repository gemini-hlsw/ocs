package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{GridBagConstraints, GridBagLayout }
import javax.swing.JPanel

import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{ TextBoxWidget, TextBoxWidgetWatcher}

import scalaz.concurrent.Task
import scalaz.syntax.id._
import scalaz.{-\/, \/-}

// Name editor, with catalog lookup for sidereal targets
final class ConicNameEditor extends JPanel with TelescopePosEditor with ReentrancyHack {

  private[this] var spt = new SPTarget // never null

  val name = new TextBoxWidget <| { w =>
    w.setColumns(20)
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {

      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant {
          spt.getTarget.setName(tbwe.getValue)
          spt.notifyOfGenericUpdate()
        }

      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        () // TODO

    })
  }

  setLayout(new GridBagLayout)

  add(name, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 0
    c.fill = GridBagConstraints.HORIZONTAL
    c.weightx = 2
  })

  def edit(ctx: GOption[ObsContext], target: SPTarget): Unit = {
    this.spt = target
    nonreentrant {
      name.setText(spt.getTarget.getName)
    }
  }

}

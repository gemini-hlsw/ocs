package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{Insets, GridBagConstraints, GridBagLayout}
import javax.swing.{JLabel, JPanel, JComponent}

import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ConicTarget
import edu.gemini.spModel.target.system.ITarget.Tag
;
import jsky.app.ot.gemini.editor.targetComponent.MagnitudeEditor
import jsky.util.gui.NumberBoxWidget
import jsky.util.gui.TextBoxWidget
import jsky.util.gui.TextBoxWidgetWatcher

import scalaz.syntax.id._

final class JplMinorBodyDetailEditor extends TargetDetailEditor(Tag.JPL_MINOR_BODY) {

  val param_epoch = new ParamPanel.ParamInfo("EPOCH", "Orbital Element Epoch (JD)")
  val param_in    = new ParamPanel.ParamInfo("IN",    "Inclination (deg)")
  val param_om    = new ParamPanel.ParamInfo("OM",    "Longitude of Ascending Node (deg)")
  val param_w     = new ParamPanel.ParamInfo("W",     "Argument of Perihelion (deg)")
  val param_qr    = new ParamPanel.ParamInfo("QR",    "Perihelion Distance (AU)")
  val param_ec    = new ParamPanel.ParamInfo("EC",    "Eccentricity")
  val param_tp    = new ParamPanel.ParamInfo("TP",    "Time of Perihelion Passage (JD)")

  val magnitudeEditor = new MagnitudeEditor {
    getComponent.asInstanceOf[JComponent].setBorder(titleBorder("Magnitudes"))
  }

  val pp = new ParamPanel("Orbital Elements",
    param_epoch, param_in, param_om, param_w, param_qr, param_ec, param_tp)

  private[this] var updating = false
  private[this] var target = new SPTarget // never null

  // Initialization
  setLayout(new GridBagLayout)

  // Add the magnitude editor
  add(magnitudeEditor.getComponent, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 0
    c.fill = GridBagConstraints.BOTH
  })

  // Add the param panel
  add(pp,  new GridBagConstraints <| { c =>
    c.gridx = 1
    c.gridy = 0
    c.fill = GridBagConstraints.HORIZONTAL
  })

  // Valid at panel

  // Add the listeners
  watch(param_epoch, _.getEpoch.setValue(_))
  watch(param_in,    _.getInclination.setValue(_))
  watch(param_om,    _.getANode.setValue(_))
  watch(param_w,     _.getPerihelion.setValue(_))
  watch(param_qr,    _.getAQ.setValue(_))
  watch(param_tp,    _.getEpochOfPeri.setValue(_))
  watch(param_ec,    _.setE(_)) // N.B. no CoordinateParam for eccentricity

  override def edit(obsContext: GOption[ObsContext], spTarget: SPTarget): Unit = {
    super.edit(obsContext, spTarget)
    magnitudeEditor.edit(obsContext, spTarget)

    // Local updates
    this.target = spTarget;
    if (!updating) {
      val ct = spTarget.getTarget.asInstanceOf[ConicTarget]
      param_epoch.widget.setValue(ct.getEpoch.getValue)
      param_in.widget   .setValue(ct.getInclination.getValue)
      param_om.widget   .setValue(ct.getANode.getValue)
      param_w.widget    .setValue(ct.getPerihelion.getValue)
      param_qr.widget   .setValue(ct.getAQ.getValue)
      param_ec.widget   .setValue(ct.getE)
      param_tp.widget   .setValue(ct.getEpochOfPeri.getValue)
    }

  }


  private def watch(pi: ParamPanel.ParamInfo, f: (ConicTarget, Double) => Unit): Unit =
    pi.widget.addWatcher(new LocalPropSetter(f))

  private class LocalPropSetter(f: (ConicTarget, Double) => Unit) extends TextBoxWidgetWatcher {
    def textBoxKeyPress(tbwe: TextBoxWidget): Unit = {
      textBoxAction(tbwe)
    }
    def textBoxAction(tbwe: TextBoxWidget): Unit = {
      try {
        updating = true
        f(target.getTarget.asInstanceOf[ConicTarget], tbwe.getValue.toDouble)
        target.notifyOfGenericUpdate();
      } catch {
        case nfe: NumberFormatException => // ignore
      } finally {
        updating = false;
      }
    }
  }


}






final class ParamPanel(title: String, nbs: ParamPanel.ParamInfo*) extends JPanel {
  import ParamPanel._
  setBorder(titleBorder(title));
  setLayout(new GridBagLayout)
  nbs.zipWithIndex.foreach { case (pi, i) =>
    add(pi.shortLabel, slc(i))
    add(pi.widget, nbc(i))
    add(pi.longLabel, llc(i))
  }
}

object ParamPanel {

  final class ParamInfo(shortLabelText: String, longLabelText: String) {
    val shortLabel = new JLabel(shortLabelText)
    val longLabel  = new JLabel(longLabelText)
    val widget = new NumberBoxWidget {
      setColumns(10)
      setMinimumSize(getPreferredSize())
    }
  }

  val ins = new Insets(0, 2, 0, 2);

  def slc(row: Int): GridBagConstraints =
    new GridBagConstraints <| { c =>
      c.gridx = 0
      c.gridy = row
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = ins
    }

  def nbc(row: Int): GridBagConstraints =
    new GridBagConstraints <| { c =>
      c.gridx = 1
      c.gridy = row
      c.insets = ins
    }

  def llc(row: Int): GridBagConstraints =
    new GridBagConstraints <| { c =>
      c.gridx = 2
      c.gridy = row
      c.fill = GridBagConstraints.HORIZONTAL
      c.weighty = 2
      c.insets = ins
    }

}
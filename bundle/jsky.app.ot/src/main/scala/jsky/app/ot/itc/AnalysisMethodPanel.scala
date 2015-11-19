package jsky.app.ot.itc

import java.awt.Insets
import javax.swing.BorderFactory

import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth
import edu.gemini.spModel.gemini.gmos.{InstGmosNorth, InstGmosSouth}
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.nifs.InstNIFS
import edu.gemini.spModel.gemini.niri.InstNIRI
import jsky.app.ot.editor.seq.EdIteratorFolder
import jsky.util.gui.{NumberBoxWidget, TextBoxWidget, TextBoxWidgetWatcher}

import scala.swing.GridBagPanel.Anchor
import scala.swing._
import scala.swing.event.{ButtonClicked, SelectionChanged, ValueChanged}
import scalaz.Scalaz._

object AnalysisMethodPanel {
  // TODO: The analysis method will have to become part of the observation model to make it persistent.
  // As an intermediate solution (to keep the values available when switching between different
  // observations for at least as long as the OT is running) we store them here in a local cache.
  val analysisCache = scala.collection.mutable.Map[SPNodeKey, AnalysisMethod]()

}

abstract class AnalysisMethodPanel(owner: EdIteratorFolder) extends GridBagPanel {

  def analysisMethod: Option[AnalysisMethod]
  def update(): Unit

  // get current nodeKey (used as lookup key for the observation)
  def nodeKey = owner.getContextObservation.getNodeKey

  // udpate the value that's currently cached for this observation
  def updateCache() = analysisMethod.foreach { AnalysisMethodPanel.analysisCache.put(nodeKey, _) }


}

/**
  * UI element that allows to enter ITC analysis method parameters for aperture analysis methods.
  */
final class AnalysisApertureMethodPanel(owner: EdIteratorFolder, fixedSkyValue: Boolean = false) extends AnalysisMethodPanel(owner) {

  val autoAperture  = new RadioButton("Auto") { focusable = false; selected = true }
  val userAperture  = new RadioButton("User") { focusable = false }
  val skyLabel      = new Label("Sky Aperture")
  val skyUnits      = new Label("x target aperture")
  val sky           = new NumberEdit(skyLabel, skyUnits, 5.0)
  val targetLabel   = new Label("Target Aperture")
  val targetUnits   = new Label("arcsec")
  val target        = new NumberEdit(targetLabel, targetUnits, 2) <| {_.peer.setEnabled(false)}
  new ButtonGroup(autoAperture, userAperture)

  layout(new Label("Analysis Method:")) = new Constraints { gridx = 0; gridy = 0; anchor = Anchor.West; gridwidth = 5; insets = new Insets(0, 0, 5, 0) }
  layout(targetLabel)                   = new Constraints { gridx = 0; gridy = 1; anchor = Anchor.West; insets = new Insets(0, 0, 0, 10) }
  layout(autoAperture)                  = new Constraints { gridx = 1; gridy = 1; insets = new Insets(0, 0, 0, 3) }
  layout(userAperture)                  = new Constraints { gridx = 2; gridy = 1; insets = new Insets(0, 0, 0, 5) }
  layout(target)                        = new Constraints { gridx = 3; gridy = 1; anchor = Anchor.West;  insets = new Insets(0, 0, 0, 3) }
  layout(targetUnits)                   = new Constraints { gridx = 4; gridy = 1; anchor = Anchor.West }
  layout(skyLabel)                      = new Constraints { gridx = 0; gridy = 2; anchor = Anchor.West; insets = new Insets(0, 0, 0, 10) }
  layout(sky)                           = new Constraints { gridx = 3; gridy = 2; anchor = Anchor.West; insets = new Insets(0, 0, 0, 3) }
  layout(skyUnits)                      = new Constraints { gridx = 4; gridy = 2; anchor = Anchor.West }

  // IR instruments (GNIRS, NIRI, F2 and GSAOI) don't allow the user to change the sky value (OCSADV-345)
  if (fixedSkyValue) {
    List(sky, skyLabel, skyUnits).foreach(_.visible = false)
  }

  listenTo(autoAperture, userAperture, target, sky)
  reactions += {
    case ButtonClicked(`autoAperture`)  => toggleUserAperture(enabled = false, "");   updateCache(); publish(new SelectionChanged(this))
    case ButtonClicked(`userAperture`)  => toggleUserAperture(enabled = true, "2.0"); updateCache(); publish(new SelectionChanged(this))
    case ValueChanged(_)                => updateCache(); publish(new SelectionChanged(this))
  }

  // update UI elements and values that are displayed
  def update() = {
    AnalysisMethodPanel.analysisCache.get(nodeKey) match {
      case Some(m: ApertureMethod) => setMethod(m)              // use cached method if it is an IFU method
      case _                       => setMethod(defaultMethod)  // otherwise fall back to default
    }
  }

  // create the analysis method for the current user entries
  def analysisMethod: Option[AnalysisMethod] =
    if (autoAperture.selected) autoApertureValue else userApertureValue

  private def autoApertureValue: Option[AnalysisMethod] =
    sky.value.map(AutoAperture)

  private def userApertureValue: Option[AnalysisMethod]  =
    for {
      sky <- sky.value
      trg <- target.value
    } yield UserAperture(trg, sky)

  private def setMethod(method: ApertureMethod): Unit = method match {
    case AutoAperture(s)    =>
      autoAperture.selected = true
      toggleUserAperture(enabled = false, "")
      sky.peer.setValue(s)
    case UserAperture(d, s) =>
      userAperture.selected = true
      toggleUserAperture(enabled = true, d.toString)
      sky.peer.setValue(s)
  }

  // generate a default analysis method for observations for which we don't have anything in the cache yet
  private def defaultMethod: ApertureMethod = owner.getContextInstrumentDataObject match {
    case _: InstNIRI   => AutoAperture(1.0)   // IR instruments use default of 1 for sky aperture
    case _: Flamingos2 => AutoAperture(1.0)
    case _: Gsaoi      => AutoAperture(1.0)
    case _: InstGNIRS  => AutoAperture(1.0)
    case _             => AutoAperture(5.0)   // default for everything else is 5
  }

  // helper to activate/deactivate the user value for the target aperture
  private def toggleUserAperture(enabled: Boolean, v: String) = {
    deafTo(userAperture)
    target.peer.setEnabled(enabled)
    targetUnits.enabled = enabled
    target.peer.setValue(v)
    listenTo(userAperture)
  }

}

/**
 * UI element that allows to enter ITC analysis method parameters for ifu analysis methods.
 */
final class AnalysisIfuMethodPanel(owner: EdIteratorFolder, summedAllowed: Boolean = true, skyEditable: Boolean = true) extends AnalysisMethodPanel(owner) {

  val summed = new RadioButton() { focusable = false; selected = true }
  val single = new RadioButton() { focusable = false }
  val radial = new RadioButton() { focusable = false }
  new ButtonGroup(summed, single, radial)

  val onSky           = new Field(1.0)
  val onSkyPanel      = new Panel {
    contents += new Label("Number of fibres used for sky")
    contents += onSky
  }

  val summedNumX      = new Field(2.0)
  val summedNumY      = new Field(5.0)
  val summedPanel     = new Panel {
    contents += new Label("Sum")
    contents += summedNumX
    contents += new Label("by")
    contents += summedNumY
    contents += new Label("IFU elements centered.")
  }

  val singleOffset    = new Field(0.0)
  val singlePanel     = new Panel {
    contents += new Label("Individual IFU element offset by")
    contents += singleOffset
    contents += new Label("arcsecs from center.")
  }

  val radialMinOffset = new Field(0.0)
  val radialMaxOffset = new Field(1.0)
  val radialPanel     = new Panel {
    contents += new Label("Multiple IFU elements along a radius with offsets of")
    contents += radialMinOffset
    contents += new Label("to")
    contents += radialMaxOffset
    contents += new Label("arcsecs.")
  }

  layout(new Label("Analysis Method:")) = new Constraints { gridx = 0; gridy = 0; anchor = Anchor.West; gridwidth = 2; insets = new Insets(0, 0, 5, 0) }
  layout(onSkyPanel)                    = new Constraints { gridx = 0; gridy = 1; anchor = Anchor.West; gridwidth = 2 }
  layout(summed)                        = new Constraints { gridx = 0; gridy = 2; anchor = Anchor.West; insets = new Insets(0, 0, 0, 10) }
  layout(summedPanel)                   = new Constraints { gridx = 1; gridy = 2; anchor = Anchor.West }
  layout(single)                        = new Constraints { gridx = 0; gridy = 3; anchor = Anchor.West; insets = new Insets(0, 0, 0, 10) }
  layout(singlePanel)                   = new Constraints { gridx = 1; gridy = 3; anchor = Anchor.West }
  layout(radial)                        = new Constraints { gridx = 0; gridy = 4; anchor = Anchor.West; insets = new Insets(0, 0, 0, 10) }
  layout(radialPanel)                   = new Constraints { gridx = 1; gridy = 4; anchor = Anchor.West }

  // NIFS does not allow to edit sky fibres (always 1)
  if (!skyEditable) {
    onSkyPanel.visible  = false
  }
  // GMOS only supports two of the three IFU modes
  if (!summedAllowed) {
    summed.visible      = false
    summedPanel.visible = false
  }

  enableFields()

  listenTo(summed, single, radial, onSky, summedNumX, summedNumY, singleOffset, radialMinOffset, radialMaxOffset)
  reactions += {
    case ButtonClicked(_) | ValueChanged(_) =>
      updateCache()
      enableFields()
      publish(new SelectionChanged(this))
  }

  // update UI elements and values that are displayed
  def update() = {
    AnalysisMethodPanel.analysisCache.get(nodeKey) match {
      case Some(m: IfuMethod) => setMethod(m)               // use cached method if it is an IFU method
      case _                  => setMethod(defaultMethod)   // otherwise fall back to default
    }
    enableFields()
  }

  // create the analysis method for the current user entries
  def analysisMethod: Option[AnalysisMethod] =
    if (summed.selected)
      for {
        sky  <- onSky.value
        numX <- summedNumX.value
        numY <- summedNumY.value
      } yield IfuSummed(sky.toInt, numX.toInt, numY.toInt, 0, 0) // TODO: currently we only support center = (0,0)
    else if (single.selected)
      for {
        sky <- onSky.value
        off <- singleOffset.value
      } yield IfuSingle(sky.toInt, off)
    else if (radial.selected)
      for {
        sky  <- onSky.value
        minO <- radialMinOffset.value
        maxO <- radialMaxOffset.value
      } yield IfuRadial(sky.toInt, minO, maxO)
    else sys.error("")

  // update UI elements for given method
  private def setMethod(method: IfuMethod): Unit = method match {
    case IfuSummed(sky, numX, numY, _, _)       =>             // TODO: currently we only support center = (0,0)
      summed.selected = true
      onSky.peer.setValue(sky)
      summedNumX.peer.setValue(numX)
      summedNumY.peer.setValue(numY)
    case IfuSingle(sky, offset)                 =>
      single.selected = true
      onSky.peer.setValue(sky)
      singleOffset.peer.setValue(offset)
    case IfuRadial(sky, minOffset, maxOffset)   =>
      radial.selected = true
      onSky.peer.setValue(sky)
      radialMinOffset.peer.setValue(minOffset)
      radialMaxOffset.peer.setValue(maxOffset)
  }

  // enable/disable input fields according to current selection
  private def enableFields(): Unit = {
    List(summedNumX, summedNumY).          foreach(_.enabled = summed.selected)
    List(singleOffset).                    foreach(_.enabled = single.selected)
    List(radialMinOffset, radialMaxOffset).foreach(_.enabled = radial.selected)
  }

  // generate a default analysis method for observations for which we don't have anything in the cache yet
  private def defaultMethod: IfuMethod = owner.getContextInstrumentDataObject match {
    // list of all currently supported IFU cases
    case i: InstGmosNorth => i.getFPUnitNorth match {
      case FPUnitNorth.IFU_1                      => IfuSingle(500, 0.0)  // GMOS-N IFU-2
      case FPUnitNorth.IFU_2 | FPUnitNorth.IFU_3  => IfuSingle(250, 0.0)  // GMOS-N IFU-B / IFU-R
      case _                                      => sys.error("not IFU")
    }
    case i: InstGmosSouth => i.getFPUnitSouth match {
      case FPUnitSouth.IFU_1                      => IfuSingle(500, 0.0)  // GMOS-S IFU-2
      case FPUnitSouth.IFU_2 | FPUnitSouth.IFU_3  => IfuSingle(250, 0.0)  // GMOS-S IFU-B / IFU-R
      case _                                      => sys.error("not IFU")
    }
    case _: InstNIFS                              => IfuSingle(  1, 0.0)  // everything else (currently NIFS only)
    case _                                        => sys.error("not IFU")
  }


}

class Panel extends FlowPanel {
  border = BorderFactory.createEmptyBorder()
  hGap = 3
  vGap = 1
}

class Field(default: Double = 0) extends Component {
  override lazy val peer = new NumberBoxWidget {
    setColumns(3)
    setValue(default)
    setMinimumSize(getPreferredSize)
    addWatcher(new TextBoxWidgetWatcher {
      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit = textBoxAction(tbwe)
      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        try {
          publish(new ValueChanged(Field.this))
        } catch {
          case _: NumberFormatException =>
        }
    })
  }
  def value: Option[Double] =
    try {
      Some(peer.getValue.toDouble)
    } catch {
      case _: NumberFormatException => None
    }
}


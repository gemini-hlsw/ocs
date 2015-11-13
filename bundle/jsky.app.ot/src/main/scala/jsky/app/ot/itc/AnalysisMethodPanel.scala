package jsky.app.ot.itc

import java.awt.Insets

import edu.gemini.itc.shared.{UserAperture, AutoAperture, AnalysisMethod}
import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth
import edu.gemini.spModel.gemini.gmos.{InstGmosSouth, InstGmosNorth}
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.niri.InstNIRI
import jsky.app.ot.editor.seq.EdIteratorFolder

import scala.swing.GridBagPanel.Anchor
import scala.swing.event.{ValueChanged, SelectionChanged, ButtonClicked}
import scala.swing.{ButtonGroup, Label, RadioButton, GridBagPanel}

import scalaz._
import Scalaz._

object AnalysisMethodPanel {
  // TODO: The analysis method will have to become part of the observation model to make it persistent.
  // As an intermediate solution (to keep the values available when switching between different
  // observations for at least as long as the OT is running) we store them here in a local cache.
  val analysisCache = scala.collection.mutable.Map[SPNodeKey, AnalysisMethod]()
}

/**
  * UI element that allows to enter ITC analysis method parameters.
  */
final class AnalysisMethodPanel(owner: EdIteratorFolder) extends GridBagPanel {

  val defaultAnalysisMethod = AutoAperture(5.0)

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

  listenTo(autoAperture, userAperture, target, sky)
  reactions += {
    case ButtonClicked(`autoAperture`)  => deafTo(userAperture); toggleUserAperture(enabled = false, "");   listenTo(userAperture); updateCache(); publish(new SelectionChanged(this))
    case ButtonClicked(`userAperture`)  => deafTo(userAperture); toggleUserAperture(enabled = true, "2.0"); listenTo(userAperture); updateCache(); publish(new SelectionChanged(this))
    case ValueChanged(_)                => updateCache(); publish(new SelectionChanged(this))
  }

  // get current nodeKey (used as lookup key for the observation)
  def nodeKey = owner.getContextObservation.getNodeKey

  // udpate the value that's currently cached for this observation
  def updateCache() = analysisMethod.foreach { AnalysisMethodPanel.analysisCache.put(nodeKey, _) }

  // generate a default analysis method for observations for which we don't have anything in the cache yet
  def defaultMethod: AnalysisMethod = owner.getContextInstrumentDataObject match {
    case i: InstGmosNorth => i.getFPUnitNorth match {
      case FPUnitNorth.IFU_1                      => AutoAperture(500)  // GMOS-N IFU-2
      case FPUnitNorth.IFU_2 | FPUnitNorth.IFU_3  => AutoAperture(250)  // GMOS-N IFU-B / IFU-R
      case _                                      => AutoAperture(1)    // GMOS-N everything else
    }
    case i: InstGmosSouth => i.getFPUnitSouth match {
      case FPUnitSouth.IFU_1                      => AutoAperture(500)  // GMOS-S IFU-2
      case FPUnitSouth.IFU_2 | FPUnitSouth.IFU_3  => AutoAperture(250)  // GMOS-S IFU-B / IFU-R
      case _                                      => AutoAperture(1)    // GMOS-S everything else
    }
    case i: InstNIRI                              => AutoAperture(1.0)
    case i: Flamingos2                            => AutoAperture(1.0)
    case i: Gsaoi                                 => AutoAperture(1.0)
    case _                                        => defaultAnalysisMethod
  }

  // helper to activate/deactivate the user value for the target aperture
  def toggleUserAperture(enabled: Boolean, v: String) = {
    target.peer.setEnabled(enabled)
    targetUnits.enabled = enabled
    target.peer.setValue(v)
  }

  // update UI elements and values that are displayed
  def update() = {
    AnalysisMethodPanel.analysisCache.getOrElse(nodeKey, defaultMethod) match {
      case AutoAperture(s) =>
        autoAperture.selected = true
        toggleUserAperture(enabled = false, "")
        sky.peer.setValue(s)
      case UserAperture(d, s) =>
        userAperture.selected = true
        toggleUserAperture(enabled = true, d.toString)
        sky.peer.setValue(s)
    }

    // special handling for NIRI, F2 and GSAOI (OCSADV-345)
    Option(owner.getContextInstrumentDataObject).foreach { _.getType match {
      case InstNIRI.SP_TYPE | Flamingos2.SP_TYPE | Gsaoi.SP_TYPE  =>
        sky.tooltip       = "This instrument does not support user defined values for the sky aperture."
        sky.enabled       = false
      case _  =>
        sky.tooltip       = null
        sky.enabled       = true
    }}
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

}

package jsky.app.ot.itc

import java.awt.{Insets, Color}

import edu.gemini.itc.shared.PlottingDetails
import edu.gemini.itc.shared.PlottingDetails.PlotLimits

import scala.swing.GridBagPanel.Fill
import scala.swing.event.{ValueChanged, SelectionChanged, ButtonClicked}
import scala.swing.{ButtonGroup, Label, RadioButton, GridBagPanel}

import scalaz._
import Scalaz._

/**
 * User element that allows to change the min and max wavelength to be displayed in ITC charts.
 */
final class PlotDetailsPanel extends GridBagPanel {

  val autoLimits      = new RadioButton("Auto") { focusable = false; background = Color.WHITE; selected = true }
  val userLimits      = new RadioButton("User") { focusable = false; background = Color.WHITE }
  val lowLimitLabel   = new Label("Low")
  val lowLimitUnits   = new Label("nm")
  val lowLimit        = new NumberEdit(lowLimitLabel, lowLimitUnits, 0)       { enabled = false }
  val highLimitLabel  = new Label("High")
  val highLimitUnits  = new Label("nm")
  val highLimit       = new NumberEdit(highLimitLabel, highLimitUnits, 2000)  { enabled = false }
  new ButtonGroup(autoLimits, userLimits)

  background = Color.WHITE
  layout(new Label("Limits:"))  = new Constraints { gridx = 0; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(autoLimits)            = new Constraints { gridx = 1; gridy = 0; insets = new Insets(0, 0, 0, 10) }
  layout(userLimits)            = new Constraints { gridx = 2; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(lowLimitLabel)         = new Constraints { gridx = 3; gridy = 0; insets = new Insets(0, 0, 0, 10) }
  layout(lowLimit)              = new Constraints { gridx = 4; gridy = 0; fill = Fill.Horizontal }
  layout(lowLimitUnits)         = new Constraints { gridx = 5; gridy = 0; insets = new Insets(0, 5, 0, 0) }
  layout(highLimitLabel)        = new Constraints { gridx = 6; gridy = 0; insets = new Insets(0, 20, 0, 10) }
  layout(highLimit)             = new Constraints { gridx = 7; gridy = 0; fill = Fill.Horizontal }
  layout(highLimitUnits)        = new Constraints { gridx = 8; gridy = 0; insets = new Insets(0, 5, 0, 0) }

  listenTo(autoLimits, userLimits, lowLimit, highLimit)
  reactions += {
    case ButtonClicked(`autoLimits`)  => lowLimit.enabled = false; highLimit.enabled = false; publish(new SelectionChanged(this))
    case ButtonClicked(`userLimits`)  => lowLimit.enabled = true;  highLimit.enabled = true;  publish(new SelectionChanged(this))
    case ValueChanged(_)              => publish(new SelectionChanged(this))
  }

  def plottingDetails: PlottingDetails =
    if (autoLimits.selected) PlottingDetails.Auto
    else userPlottingDetails.getOrElse(PlottingDetails.Auto)

  private def userPlottingDetails: Option[PlottingDetails] =
    for {
      low    <- lowLimit.value
      high   <- highLimit.value
      (l, h) <- if (low < high) Some((low, high)) else None
    } yield new PlottingDetails(PlotLimits.USER, l, h)

}

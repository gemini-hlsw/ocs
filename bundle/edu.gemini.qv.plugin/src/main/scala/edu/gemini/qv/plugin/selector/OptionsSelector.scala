package edu.gemini.qv.plugin.selector

import edu.gemini.qv.plugin.selector.OptionsSelector._
import edu.gemini.qv.plugin.ui.SideBarPanel
import edu.gemini.util.skycalc.calc.TargetCalculator
import javax.swing.BorderFactory
import javax.swing.border.EtchedBorder
import scala.swing.GridBagPanel.Fill._
import scala.swing._
import scala.swing.event.{ButtonClicked, Event}

object OptionsChanged extends Event

/**
 * GUI element that allows to display an arbitrary number of panels with options to select from.
 * Options on a panel can either be mutually exclusive or not and are rendered accordingly as radio buttons
 * or check boxes. Radio buttons are put in a button group. The whole options selector implements a side
 * bar which can be added to the side of the charts for which the options are relevant.
 * @param groups
 */
class OptionsSelector(groups: Group*) extends SideBarPanel("Options") {

  private val panels = groups.map(g => OptionsGroup(g.label, g.tip, g.options, g.mutex))

  def selected = panels.foldLeft(Set[ChartOption]())(_ ++ _.selected)
  def isSelected(detail: ChartOption) = selected.contains(detail)
  def markers() = selected.filter(_.isInstanceOf[Marker])
  def curves = selected.filter(_.isInstanceOf[Curve])
  def aggregates() = selected.filter(_.isInstanceOf[Aggregate])

  panels.zipWithIndex.foreach { case (p, y) =>
    layout(p) = new Constraints {
      gridx = 0
      gridy = y
      weightx = 1
      fill = Horizontal
    }
  }
  layout(Swing.VGlue) = new Constraints {
    gridx = 0
    gridy = panels.size
    weighty = 1
    fill = Vertical
  }

  // == helper classes...

  trait OptionButton extends ToggleButton {
    val option: ChartOption
  }

  class OptionsGroup(label: String, tip: String, buttons: Seq[OptionButton]) extends BoxPanel(Orientation.Vertical) {
    border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), label)
    tooltip = tip
    buttons.foreach(contents += _)

    def selected = buttons.filter(_.selected).map(_.option).toSet

    listenTo(buttons:_*)
    reactions += {
      case ButtonClicked(_) => OptionsSelector.this.publish(OptionsChanged)
    }
  }

  class OptionsRadioButton(val option: ChartOption) extends RadioButton(option.label) with OptionButton {
    tooltip = option.tip
    selected = option.selected
  }

  class OptionsCheckBox(val option: ChartOption) extends CheckBox(option.label) with OptionButton  {
    tooltip = option.tip
    selected = option.selected
  }

  object OptionsGroup {

    def apply(label: String, tip: String, options: Seq[ChartOption], mutex: Boolean = false) = {
      val buttons = mutex match {
        case true => options.map(new OptionsRadioButton(_))
        case false => options.map(new OptionsCheckBox(_))
      }
      if (mutex) new ButtonGroup(buttons:_*)
      new OptionsGroup(label, tip, buttons)
    }

  }

}

object OptionsSelector {

  case class Group(label: String, tip: String, mutex: Boolean, options: ChartOption*)

  trait ChartOption {
    val selected: Boolean
    val tip: String
    val label: String
  }

  case class Marker(label: String, tip: String, selected: Boolean = false) extends ChartOption
  object Now extends Marker("Now", """Show a marker for "now" in the plot.""", selected = true)
  object Twilights extends Marker("Twilights", "Show markers for sunrise and sunset and nautical twilights.", selected = true)
  object MoonPhases extends Marker("Moon Phases", "Show moon phases.")
  object IgnoreDaytime extends Marker("Ignore Daytime", "Fold segmented solutions for the same night into a single solution that covers the whole night.")
  object MoonElevation extends Marker("Moon Elevation", "Show the moon elevation.")
  object MoonHours extends Marker("Moon Hours", "Show the number of hours the moon is up.")
  object MoonSetRise extends Marker("Moon Set/Rise Time", "Show the set or rise time of the moon.")
  object AirmassRuler extends Marker("Airmass", "Indicate airmass instead of elevation in degrees.")
  object InsideMarkers extends Marker("Highlight Observable", "Hightlight areas where the observation (or any observation) is observable in green.")
  object OutsideMarkers extends Marker("Highlight Not Observable", "Highlight areas where the observation (or none of the observations) is not observable in red.")
  object Schedule extends Marker("Schedule", "Show the schedule constraints.", selected = true)
  object DarkHours extends Marker("Dark Hours", "Show the dark hours.")
  object AvailableHours extends Marker("Available Hours", "Show available hours.")
  object EmptyCategories extends Marker("Show Empty Categories", "Show or hide categories for which there are no values.")
  object RaAsLst extends Marker("Show RA as LST", "Show local sidereal time instead of right ascension.")


  import TargetCalculator.Fields._

  case class Curve(label: String, tip: String, field: Field, selected: Boolean = false) extends ChartOption
  object ElevationCurve extends Curve("Elevation", "Plot the elevation for all active observations.", Elevation)
  object ParallacticAngleCurve extends Curve("Parallactic Angle", "Plot the parallactiv angle.", ParallacticAngle)
  object SkyBrightnessCurve extends Curve("Sky Brightness", "Plot the sky brightness.", SkyBrightness)
  object LunarDistanceCurve extends Curve("Lunar Distance", "Plot the angular distance between the observation's target and the moon.", LunarDistance)
  object HourAngleCurve extends Curve("Hour Angle", "Plot the hour angle.", HourAngle)

  case class Aggregate(label: String, tip: String, selected: Boolean = false) extends ChartOption
  object MinElevationCurve extends Aggregate("Min Elevation", "Plot the minimal elevation for all active observations.")
  object MaxElevationCurve extends Aggregate("Max Elevation", "Plot the maximal elevation for all active observations.")

  case class RiseSetOption(label: String, tip: String, selected: Boolean = false) extends ChartOption
  object ShowSetTimeOption extends RiseSetOption("Set", "", selected = true)
  object ShowRiseTimeOption extends RiseSetOption("Rise", "")
}



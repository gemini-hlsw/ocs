package edu.gemini.qv.plugin.filter.ui

import edu.gemini.qv.plugin.data.{DataChanged, ObservationProvider}
import edu.gemini.qv.plugin.filter.core.Filter._
import edu.gemini.qv.plugin.filter.core._
import edu.gemini.qv.plugin.ui.QvGui
import java.awt.{Color, Font}
import javax.swing.BorderFactory
import scala.Some
import scala.swing.GridBagPanel.Fill._
import scala.swing.GridBagPanel.Anchor._
import scala.swing._
import scala.swing.event._

/**
 * All filter ui elements are Swing components which can provide a filter based on the current user settings.
 * They all publish FilterElementChanged events in case the filter has been changed.
 */
trait FilterUI extends Component {
  def filter: Filter
}

/**
 * Some Swing based UI elements that represent different filter elements.
 */
object FilterElement {

  /** Notify external listeners of a change in the filter. */
  case object FilterElementChanged extends Event
  case object FilterElementChanged2 extends Event   // TODO : Better names..
  /** Notify internal listeners of a change. */
  private case object ElementChanged extends Event

  private val Inactive = Color.gray
  private val Active = QvGui.Green

  class Combi(data: ObservationProvider, elements: Seq[FilterUI]) extends GridBagPanel with FilterUI {

    border = BorderFactory.createEmptyBorder(2,0,0,0)

    // create default constraints
    private val constraints = new Constraints {
      weightx = 1.0
      gridx = 0
      gridy = 0
      fill = Horizontal
    }
    // layout and add filter elements vertically in order of sequence (except for last one)
    private var ypos = 1
    elements.dropRight(1).foreach({ e =>
      constraints.gridy = ypos
      layout(e) = constraints
      ypos = ypos + 1
    })
    // make last component eat up all remaining vertical space
    elements.takeRight(1).foreach({ e =>
      constraints.anchor = FirstLineStart
      constraints.weighty = 1.0
      constraints.gridy = ypos
      layout(e) = constraints
    })


    listenTo(elements:_*)
    reactions += {
      // propagate filter changed of child element to whoever listens to combi element
      case ElementChanged =>  publish(FilterElementChanged)
    }

    // get a single filter out of all elements in this combi element
    // note: the filter is simplified as much as possible by omiting "empty" filters, i.e.
    // filters that filter everything (and return an empty result)
    def filter = {
      val nonEmptyFilters = elements.map(_.filter).filter(!_.isEmpty)
      nonEmptyFilters.size match {
        case 0 => new EmptyFilter
        case 1 => nonEmptyFilters(0)
        case _ => nonEmptyFilters.reduce(_.and(_))
      }
    }

  }

  /**
   * UI element to represent options filters as a bunch of on/off (green/red) buttons for manual selection.
   * @param init the filter to start with
   * @tparam A
   */
  class Options[A](data: ObservationProvider, init: EnumFilter[A], var showAvailableOnly: Boolean = true, val showCounts: Boolean = true) extends GridBagPanel with FilterUI {
    border = BorderFactory.createEmptyBorder(2, 2, 2, 2) // add some space at top and bottom
    var selection: Set[A] = init.selection.toSet
    private val buttons = init.sortedValues.map(button)

    peer.setComponentPopupMenu(createPopup.peer)

    private val constraints = new Constraints {
      weightx = 1.0
      gridx = 0
      gridy = 0
      fill = Horizontal
    }
    layout(new Label(init.label, null, Alignment.Left)) = constraints
    private var ypos = 1
    buttons.foreach({ b =>
      constraints.gridy = ypos
      layout(b) = constraints
      ypos = ypos + 1
    })

    def filter: Filter = init.updated(selection = selection)
    def isSelected(v: A): Boolean = selection.contains(v)

    deafTo(this) // avoid cycles when republishing!
    buttons.map(listenTo(_))
    reactions += {
      // forward change events
      case ElementChanged =>  publish(ElementChanged)
    }

    private def bgColor(v: A) = if(isSelected(v)) Active else Inactive
    private def fgColor(v: A) = Color.white

    private def button(value: A) = new OptButton(init.valueName(value), value)

    private class OptButton(label: String, value: A) extends Button(label) {

      focusable = false
      background = bgColor(value)
      foreground = fgColor(value)
      font = new Font(font.getName, Font.BOLD, font.getSize)
      listenTo(data, mouse.clicks)
      updateAvailability()

      reactions += {
        case e: MouseClicked => {
          selection = if(isSelected(value)) selection - value else selection + value
          background = bgColor(value)
          foreground = fgColor(value)
          publish(ElementChanged)
        }
        case DataChanged => updateAvailability()
      }

      def updateAvailability() {
        val available = data.presentValuesWithCount(init.collector)
        val present = available.contains(value)
        text = if (present && showCounts) s"$label   (${available(value)})" else label
        visible = present || !showAvailableOnly
      }
    }

    private def createPopup = new PopupMenu {
      add(new MenuItem(new Action(label) { def apply() = {
        showAvailableOnly = !showAvailableOnly
        title = label
        buttons.foreach(_.updateAvailability())
      }}))
      def label = if (showAvailableOnly) "Show All Options" else "Show Available Options Only"
    }
  }

  class RemainingTime(init: RemainingTimeFilter) extends GridBagPanel with FilterUI {
    tooltip = init.desc
    border = BorderFactory.createEmptyBorder(0, 2, 2, 2) // add some space at top and bottom
    var filter: Filter = init

    private object min extends TextField { text = init.min.toString; enabled = init.enabled }
    private object max extends TextField { text = init.max.toString; enabled = init.enabled }
    private object filterEnabled extends CheckBox(init.label) {
      tooltip = init.desc
      foreground = QvGui.Text
      focusable = false
      selected = init.enabled
      horizontalTextPosition = Alignment.Left
    }
    private object thisSemester extends CheckBox("This") {
      tooltip = "Include all nights of the current semester."
      focusable = false
      selected = init.thisSemester
      enabled = init.enabled
    }
    private object nextSemester extends CheckBox("Next") {
      tooltip = "Include all nights of the next semester."
      focusable = false
      selected = init.nextSemester
      enabled = init.enabled
    }
    private object semesters extends BoxPanel(Orientation.Horizontal) {
      contents += thisSemester
      contents += Swing.HStrut(5)
      contents += nextSemester
    }

    // == long live the GridBagPanel! :)
    layout(filterEnabled) = new Constraints {
      gridx = 0
      gridy = 0
      weightx = 0.33
      fill = Horizontal
      anchor = LineStart
    }
    layout(new Label("min:  ", null, Alignment.Right)) = new Constraints {
      gridx = 1
      gridy = 0
      weightx = 0.33
      fill = Horizontal
    }
    layout(min) = new Constraints {
      gridx = 2
      gridy = 0
      weightx = 0.33
      fill = Horizontal
    }
    layout(new Label("max:  ", null, Alignment.Right)) = new Constraints {
      gridx = 1
      gridy = 1
      weightx = 0.33
      fill = Horizontal
    }
    layout(max) = new Constraints {
      gridx = 2
      gridy = 1
      weightx = 0.3
      weightx = 0.33
      fill = Horizontal
    }
    layout(new Label("Semesters:  ", null, Alignment.Right)) = new Constraints {
      gridx = 1
      gridy = 2
      weightx = 0.33
      fill = Horizontal
    }
    layout(semesters) = new Constraints {
      gridx = 2
      gridy = 2
      weightx = 0.3
      fill = Horizontal
      anchor = LastLineEnd
    }

    listenTo(min, max, filterEnabled, thisSemester, nextSemester)

    reactions += {
      case ButtonClicked(`filterEnabled`) =>
        min.enabled = filterEnabled.selected
        max.enabled = filterEnabled.selected
        thisSemester.enabled = filterEnabled.selected
        nextSemester.enabled = filterEnabled.selected
        doUpdate()
      case ButtonClicked(_) => doUpdate()
      case ValueChanged(_) => doUpdate()
    }

    def doUpdate() {
      val (a, b) = {
        val a = init.minFromString(min.text)
        val b = init.maxFromString(max.text)
        if (a < b) (a, b) else (a, init.highest)
      }
      filter = init match {
        case f: RemainingNights => RemainingNights(f.ctx, a, b, filterEnabled.selected, thisSemester.selected, nextSemester.selected)
        case f: RemainingHours => RemainingHours(f.ctx, a, b, filterEnabled.selected, thisSemester.selected, nextSemester.selected)
        case f: RemainingHoursFraction => RemainingHoursFraction(f.ctx, a, b, filterEnabled.selected, thisSemester.selected, nextSemester.selected)
      }
      publish(ElementChanged)
    }
  }

  /** UI element for range filters (min-max). */
  class Range(init: SimpleRangeFilter) extends GridPanel(2, 3) with FilterUI {
    tooltip = init.desc
    border = BorderFactory.createEmptyBorder(0, 2, 2, 2) // add some space at top and bottom
    var filter: Filter = init

    private object min extends TextField { text = init.min.toString }
    private object max extends TextField { text = init.max.toString }

    contents += new Label(init.label, null, Alignment.Left)
    contents += new Label("min:  ", null, Alignment.Right)
    contents += min
    contents += new Label()
    contents += new Label("max:  ", null, Alignment.Right)
    contents += max

    listenTo(min, max)

    reactions += {
      case ValueChanged(_) => doUpdate()
    }

    def doUpdate() {
      val a = init.minFromString(min.text)
      val b = init.maxFromString(max.text)
      filter = init match {
        case f: RA => RA(a, b)
        case f: Dec => Dec(a, b)
        case f: SetTime => SetTime(f.ctx, a, b)
      }
      publish(ElementChanged)
    }
  }

  /** UI element for a boolean value. */
  class Booleans(init: BooleanFilter) extends GridBagPanel with FilterUI {
    private val label = new Label(init.label)
    private val both = new RadioButton("Both") { selected = init.value == scala.None }
    private val onlyTrue = new RadioButton("Yes") { selected = init.value == Some(true) }
    private val onlyFalse = new RadioButton("No") { selected = init.value == Some(false) }
    new ButtonGroup(both, onlyTrue, onlyFalse)
    border = BorderFactory.createEmptyBorder(0, 2, 2, 2)
    private val btnPanel = new BoxPanel(Orientation.Horizontal) {
      contents += both
      contents += Swing.HStrut(5)
      contents += onlyTrue
      contents += Swing.HStrut(5)
      contents += onlyFalse
      }
    layout(label) = new Constraints {
      gridx = 0
      gridy = 0
      weightx = 0.7
      fill = Vertical
      anchor = West
    }
    layout(btnPanel)= new Constraints {
      gridx = 1
      gridy = 0
      weightx = 0.3
      fill = Vertical
      anchor = East
    }

    listenTo(both, onlyTrue, onlyFalse)
    reactions += {
      case _: ButtonClicked => publish(ElementChanged)
    }

    def filter = init match {
      case IsActive(_)                => IsActive(selection)
      case IsCompleted(_)             => IsCompleted(selection)
      case IsRollover(_)              => IsRollover(selection)
      case HasTimingConstraints(_)    => HasTimingConstraints(selection)
      case HasElevationConstraints(_) => HasElevationConstraints(selection)
      case HasPreImaging(_)           => HasPreImaging(selection)
      case HasNonSidereal(_)          => HasNonSidereal(selection)
      case HasDummyTarget(_)          => HasDummyTarget(selection)
    }

    private def selection: Option[Boolean] =
      if (both.selected) scala.None
      else if (onlyTrue.selected) Some(true)
      else Some(false)
  }

  /** UI element for a string value. */
  class Strings(init: StringFilter) extends GridPanel(1, 2) with FilterUI {
    private val textfield = new TextField { text = init.value }
    border = BorderFactory.createEmptyBorder(0, 2, 2, 2)
    contents += new Label(init.label, null, Alignment.Left)
    contents += textfield

    listenTo(textfield)
    reactions += {
      case _: ValueChanged => publish(ElementChanged)
    }

    def filter = init match {
      case ProgId(_) => ProgId(textfield.text)
      case ProgPi(_) => ProgPi(textfield.text)
      case ProgContact(_) => ProgContact(textfield.text)
      case ObsId(_) => ObsId(textfield.text)
    }
  }
}

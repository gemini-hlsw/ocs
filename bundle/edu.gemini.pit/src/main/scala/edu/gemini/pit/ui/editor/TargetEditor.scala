package edu.gemini.pit.ui.editor

import com.jgoodies.forms.factories.Borders.DLU4_BORDER
import edu.gemini.model.p1.immutable.TooTarget

import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.immutable.EphemerisElement
import edu.gemini.model.p1.immutable.NonSiderealTarget
import edu.gemini.model.p1.immutable.ProperMotion
import edu.gemini.model.p1.immutable.Semester
import edu.gemini.model.p1.immutable.SiderealTarget
import edu.gemini.model.p1.immutable.Target
import edu.gemini.pit.ui.util._
import edu.gemini.pit.ui.util.RATextField
import edu.gemini.pit.ui.util.ScrollPanes
import edu.gemini.pit.ui.util.SharedIcons
import edu.gemini.pit.ui.util.StdModalEditor
import edu.gemini.shared.gui.textComponent.{NumberField, SelectOnFocus}
import edu.gemini.spModel.core.{Magnitude, MagnitudeSystem, MagnitudeBand}
import edu.gemini.spModel.core.{Declination, RightAscension, Coordinates}
import edu.gemini.ui.gface.GComparator
import edu.gemini.ui.gface.GSelection
import edu.gemini.ui.gface.GSelectionBroker
import edu.gemini.ui.gface.GTableViewer
import edu.gemini.ui.gface.GViewer
import edu.gemini.ui.workspace.util.Factory

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

import java.awt
import edu.gemini.pit.ui.util.ToolButton
import swing._
import scala.swing.event.{ButtonClicked, ValueChanged, SelectionChanged}
import javax.swing.{Icon, BorderFactory, ListSelectionModel}
import java.util.{TimeZone, Date}
import java.text.{SimpleDateFormat, DecimalFormat}

import scalaz._
import Scalaz._

object TargetEditor {

  def open(sem:Semester, t0:Option[Target], canEdit:Boolean, parent:UIElement):Option[Target] = {
    val target = t0.getOrElse(Target.empty)
    new TargetEditor(sem, target, canEdit).open(parent) match {
      case Some(Replace(t)) => open(sem, Some(t.withUuid(target.uuid)), canEdit, parent)
      case Some(Done(t))    => Some(t)
      case None             => None
    }

  }

  sealed trait Result

  case class Replace(t:Target) extends Result

  case class Done(t:Target) extends Result

}

import TargetEditor._

/**
 * Modal editor for a Target.
 */
class TargetEditor private (semester:Semester, target:Target, canEdit:Boolean) extends StdModalEditor[Result]("Edit Target") { dialog =>
  // An ADT for our target type radio buttons
  sealed trait TargetType {
    val name: String
  }
  object TargetType {
    val all:List[TargetType] = List(SiderealType, NonSiderealType, TooType)
  }
  case object SiderealType extends TargetType {
    val name = "Sidereal"
  }
  case object NonSiderealType extends TargetType {
    val name = "Non-Sidereal"
  }
  case object TooType extends TargetType {
    val name = "Target of Opportunity"
  }


  // Construct our header and content
  override def header = Header
  def editor = Tabs

  // Initial focus
  Header.Name.requestFocus()

  // Editable
  Contents.Footer.OkButton.enabled = canEdit

  // Unlike other editors, let's not have a default button
  dialog.peer.getRootPane.setDefaultButton(null)

  // When is it valid?
  override def editorValid = Header.Name.valid && (Header.TypePicker.selection match {
    case SiderealType    =>
      Tabs.CoordinatesPageContent.RA.valid &&
        Tabs.CoordinatesPageContent.Dec.valid &&
      (!Tabs.CoordinatesPageContent.PMCheck.selected || (Tabs.CoordinatesPageContent.DeltaRA.valid && Tabs.CoordinatesPageContent.DeltaDec.valid)) &&
        Tabs.magControls.filter(_.check.selected).forall(_.text.valid)
    case NonSiderealType => true
    case TooType         => true
  })

  // Validation listeners (text)
  (List(
    Header.Name,
    Tabs.CoordinatesPageContent.RA,
    Tabs.CoordinatesPageContent.Dec,
    Tabs.CoordinatesPageContent.DeltaRA,
    Tabs.CoordinatesPageContent.DeltaDec
    ) ++ Tabs.magControls.map(_.text)) foreach {
      _.reactions += {
        case ValueChanged(_) => validateEditor()
      }
    }

  Header.TypePicker.bg.buttons.foreach {
    _.reactions += {
      case SelectionChanged(_) => validateEditor()
    }
  }
  (Tabs.CoordinatesPageContent.PMCheck :: Tabs.magControls.map(_.check)) foreach {
    _.reactions += {
      case _ => validateEditor()
    }
  }
  // Show the currently selected dab
  Tabs.switchType(Header.TypePicker.selection)

  // Our editor holds three targets, one of each type.
  lazy val (sidereal, nonSidereal, too, initialType) = {
    val st = SiderealTarget.empty
    val nt = NonSiderealTarget.empty
    val tt = TooTarget.empty
    target match {
      case t0:SiderealTarget    => (t0, nt, tt, SiderealType)
      case t0:NonSiderealTarget => (st, t0, tt, NonSiderealType)
      case t0:TooTarget         => (st, nt, t0, TooType)
    }
  }

  // Our header, which just contains the name and target type combo.
  object Header extends GridBagPanel with Rows {

    // Add our controls, which are defined below.
    addRow(new Label("Name:"), new BorderPanel {
      add(Name, BorderPanel.Position.Center)
      add(new BorderPanel {
        add(lookup, BorderPanel.Position.West)
        add(cats, BorderPanel.Position.East)
      }, BorderPanel.Position.East)
    })
    addRow(new Label("Type:"), TypePicker)

    // The target name is a simple text field.
    object Name extends TextField(target.name, 15) with SelectOnFocus with NonEmptyText {
      enabled = canEdit
    }

    // Lookup button, for synchronous lookup
    lazy val lookup = Button("Lookup...") {
      SynchronousLookup2.open(Name.text, dialog).map(Replace.apply).foreach(dialog.close)
    }
    lookup.enabled = canEdit

    // Lookup button, for synchronous lookup w/catalogs
    lazy val cats = Button("Catalogs...") {
      SynchronousLookup.open(Name.text, dialog).map(Replace.apply).foreach(dialog.close)
    }
    cats.enabled = canEdit

    // Target type picker is a group of radio buttons.
    // When the user selects a new target type, we ask the
    // tab pane to switch the set of displayed tabs.
    object TypePicker extends BoxPanel(Orientation.Horizontal) {
      val icons = Map[TargetType, Icon](
                      SiderealType    -> SharedIcons.ICON_SIDEREAL,
                      NonSiderealType -> SharedIcons.ICON_NONSIDEREAL,
                      TooType         -> SharedIcons.ICON_TOO)

      // Need to keep a var to store the current selection
      private var selectedType: TargetType = initialType
      // Gives access to the selected target type
      def selection = selectedType

      // In Swing, RadioButtons with icons don't look right, use a Label next to the radio button instead
      val targetTypeRadioButtons:List[(AbstractButton, Label)] = TargetType.all.flatMap { v =>
        val rb = new RadioButton("") {
          enabled = canEdit
          selected = initialType == v
          reactions += {
            case ButtonClicked(_) =>
              selectedType = v
              Tabs.switchType(v)
              lookup.enabled = canEdit && (v != TooType)
          }
        }
        icons.get(v).map { i =>
          val label = new Label(v.name, i, Alignment.Left)
          (rb, label)
        }
      }

      // But the radio buttons on a group
      val bg = new ButtonGroup(targetTypeRadioButtons.map(_._1).toSeq :_*) {
        enabled = canEdit
      }

      contents ++= targetTypeRadioButtons.map {
        case (r, l) =>
          new BorderPanel() {
            border = BorderFactory.createEmptyBorder(1, 10, 0, 5)
            add(r, BorderPanel.Position.West)
            add(l, BorderPanel.Position.Center)
          }
      }
    }

  }

  // Out tab panel, which contains two tabs when we're editing a sidereal target, and one tab
  // when we're editing a non-sidereal target.
  object Tabs extends TabbedPane {

    // Tweak the behavior a little
    peer.setFocusable(false)

    // A method for switching modes. The way things are engineered we'll never get called unless
    // the mode is actually changing (otherwise we would want to do verify this first).
    def switchType(t: TargetType) {
      pages.clear()
      t match {
        case SiderealType    => pages += CoordinatesPage; pages += MagnitudesPage
        case NonSiderealType => pages += Ephemeris
        case TooType         => () // there is no page for this one
      }
      visible = pages.nonEmpty
      dialog.pack()
    }

    // A page for sidereal coordinates.
    object CoordinatesPage extends TabbedPane.Page("Coordinates", new BorderPanel {

      // Add our content, which is defined below.
      add(CoordinatesPageContent, BorderPanel.Position.North)

    })

    object CoordinatesPageContent extends GridBagPanel with Rows {

      // Add our rows. Controls are defined below.
      addRow(new Label("RA:"), RA)
      addRow(new Label("Dec:"), Dec)
      addRow(new Label("Epoch:"), Epoch)
      addRow(PMCheck)
      addRow(new Label("Delta RA:") {
        enabled = PMCheck.selected && canEdit
        PMCheck.reactions += {
          case _ => enabled = PMCheck.selected && canEdit
        }
      }, DeltaRA)
      addRow(new Label("Delta Dec:") {
        enabled = PMCheck.selected && canEdit
        PMCheck.reactions += {
          case _ => enabled = PMCheck.selected && canEdit
        }
      }, DeltaDec)

      // Our initial coordinates (if any), as of the middle of the semester
      lazy val coords = target.coords(semester.midPoint)

      // Target RA and Dec in degrees
      object RA extends RATextField(coords.map(_.ra).getOrElse(RightAscension.zero)) {
        enabled = canEdit
      }

      object Dec extends DecTextField(coords.map(_.dec).getOrElse(Declination.zero)) {
        enabled = canEdit
      }

      // Epoch
      object Epoch extends ComboBox(CoordinatesEpoch.values.toSeq) {
        selection.item = target.epoch
        enabled = false // UX-691
      }

      // PM Checkbox
      object PMCheck extends CheckBox("Proper Motion") {
        selected = sidereal.properMotion.isDefined
        enabled = canEdit
      }

      // PM dRA
      object DeltaRA extends NumberField(sidereal.properMotion.map(_.deltaRA), allowEmpty = true) {
        enabled = sidereal.properMotion.isDefined && canEdit
        PMCheck.reactions += {
          case _ => enabled = PMCheck.selected && canEdit
        }
      }

      // PM dRA
      object DeltaDec extends NumberField(sidereal.properMotion.map(_.deltaDec), allowEmpty = true) {
        enabled = sidereal.properMotion.isDefined && canEdit
        PMCheck.reactions += {
          case _ => enabled = PMCheck.selected && canEdit
        }
      }

    }

    // A tab page for sidereal target magnitudes.
    object MagnitudesPage extends TabbedPane.Page("Magnitudes", new GridBagPanel with Rows {

      // Add a set of three controls for each magnitude band.
      magControls.foreach {mc =>
        addRow(mc.check, mc.text, mc.system)
      }

    })

    val magControls = allowedBands.map(b => new MagControl(b))

    // Set of three controls that allow the user to select and edit a Magnitude.
    class MagControl(val band:MagnitudeBand) {

      private val mag = sidereal.magnitudes.find(_.band == band)

      // The checkbox
      val check = new CheckBox(band.name) {
        selected = mag.isDefined
        enabled = canEdit
      }

      // The input text
      val text = new NumberField(mag.map(_.value), allowEmpty = true) {
        enabled = mag.isDefined && dialog.canEdit
        check.reactions += {
          case _ if check.selected => enabled = canEdit; requestFocus()
          case _                   => enabled = false
        }
      }

      // The system combo
      val system = new ComboBox(MagnitudeSystem.allForOT) {
        val magDefault = band.defaultSystem
        val magSys = mag.map(_.system).getOrElse(magDefault)
        enabled = mag.isDefined && canEdit
        selection.item = magSys
        check.reactions += {
          case _ => enabled = check.selected && canEdit
        }
      }

      // Get the edited magnitude, if any
      def magnitude = check.selected match {
        case false => None
        case true  => Some(new Magnitude(text.text.toFloat, band, system.selection.item))
      }

    }

    // A tab page for an ephemeris for non-sidereal targets
    object Ephemeris extends TabbedPane.Page("Ephemeris", EphemerisContent)

    object EphemerisContent extends BorderPanel {
      border = DLU4_BORDER

      // Add our contents, defined below.
      add(Header, BorderPanel.Position.North)
      add(Scroll, BorderPanel.Position.Center)
      add(Toolbar, BorderPanel.Position.South)

      // Our header is just the epoch drop-down
      object Header extends GridBagPanel with Rows {

        // Content, defined below.
        addRow(new Label("Epoch:"), NsEpoch)

      }

      // Our epoch dropdown.
      object NsEpoch extends ComboBox(CoordinatesEpoch.values) {
        selection.item = target.epoch
        enabled = false // UX-691
      }

      // Columns
      object Column extends Enumeration {
        val RA, Dec, UTC, Mag = Value
        type Column = Value
      }

      import Column._

      // Controller
      object Controller extends ListTableController[EphemerisElement, Column] {
        val utc = {
          val df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
          df.setTimeZone(TimeZone.getTimeZone("UTC"))
          df
        }

        val magFormat = new DecimalFormat("##0.000")

        val raFormat = DegreePreference.BOX.get match {
          case DegreePreference.DEGREES => DegreeFormatter.ra("")
          case DegreePreference.HMSDMS  => new HMSFormatter("")
        }

        val decFormat = DegreePreference.BOX.get match {
          case DegreePreference.DEGREES => DegreeFormatter.dec("")
          case DegreePreference.HMSDMS  => new DecFormatter("")
        }

        def getSubElement(e:EphemerisElement, c:Column) = c match {
          case RA  => raFormat.toString(e.coords.ra)
          case Dec => decFormat.toString(e.coords.dec)
          case UTC => utc.format(new Date(e.validAt))
          case Mag => e.magnitude.map(magFormat.format).orNull
        }
      }

      // Our table viewer manages a list of ephemeris elements
      object Viewer extends GTableViewer[List[EphemerisElement], EphemerisElement, Column](Controller) {
        setColumns(RA, Dec, UTC, Mag)
        setColumnSize(RA, 100)
        setColumnSize(Dec, 100)
        setColumnSize(UTC, 160, Integer.MAX_VALUE)
        setColumnSize(Mag, 50)
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        def selection = Option(getSelection).filter(!_.isEmpty).map(_.first)
        def selection_=(s:Option[EphemerisElement]) {
          setSelection(s.map(new GSelection(_)).getOrElse(GSelection.emptySelection[EphemerisElement]))
        }

        def onDoubleClick(f:EphemerisElement => Unit) {
          getTable.addMouseListener(new awt.event.MouseAdapter {
            override def mouseClicked(e:awt.event.MouseEvent) {
              for {
                z <- selection if e.getClickCount == 2 && canEdit
              } f(z)
            }
          })
        }

        def onSelectionChanged(f:Option[EphemerisElement] => Unit) {
          addPropertyChangeListener(GSelectionBroker.PROP_SELECTION, new PropertyChangeListener {
            def propertyChange(e:PropertyChangeEvent) {
              f(selection)
            }
          })
        }

        setComparator(new GComparator[List[EphemerisElement], EphemerisElement] {
          type E = EphemerisElement
          def compare(a:E, b:E) = a.validAt.compare(b.validAt)
          def modelChanged(v:GViewer[List[E], E], old:List[E], m:List[E]) {
            ()
          }
        })

        setModel(nonSidereal.ephemeris)

        // Edit on double-click
        onDoubleClick {e =>
          val m = getModel
          for {
            e0 <- new EphemerisElementEditor(e).open(dialog)
            i = m.indexOf(e) if i >= 0 // this will be the case unless something weird happened
          } {
            setModel(m.take(i) ++ (e0 :: m.drop(i + 1)))
            selection = Some(e0)
          }
        }

      }

      // Scrollpane for the table. This is kind of a pain. Blame Swing
      object Scroll extends ScrollPane {
        override lazy val peer = {
          val p = Factory.createStrippedScrollPane(Viewer.getTable)
          ScrollPanes.setViewportWidth(p)
          ScrollPanes.setViewportHeight(p, 5)
          p
        }
      }

      // Our toolbar with add/remove
      object Toolbar extends BasicToolbar {

        // Add controls, which are defined below
        add(Add)
        add(Del)

        // Our add button, which is always on
        object Add extends ToolButton(SharedIcons.ADD, SharedIcons.ADD_DISABLED, "Add Ephemeris Element") {
          override def apply() {

            enabled = canEdit

            val defaultDay = {
              val now = System.currentTimeMillis()
              (now < semester.firstDay || now > semester.lastDay) ? semester.firstDay | now
            }

            for {
              e <- new EphemerisElementEditor(EphemerisElement.empty.copy(validAt = defaultDay)).open(dialog)
              m = Viewer.getModel
            } {
              Viewer.setModel(m ++ List(e))
              Viewer.selection = Some(e)
            }

          }
        }

        // And delete button, which is enabled only if there's a selection in the viewer
        object Del extends ToolButton(SharedIcons.REMOVE, SharedIcons.REMOVE_DISABLED, "Delete Ephemeris Element") {
          override def apply() {
            val m = Viewer.getModel
            for {
              e <- Viewer.selection
              i = m.indexOf(e) if i >= 0
            } {
              Viewer.setModel(m.take(i) ++ m.drop(i + 1))
              Viewer.selection = None
            }
          }
          enabled = false
          Viewer.onSelectionChanged {oe => enabled = oe.isDefined && canEdit}
        }

      }

    }

  }

  def value = Done(value0)

  def value0 = Header.TypePicker.selection match {
    case SiderealType =>

      import Header.Name
      import Tabs.CoordinatesPageContent._

      sidereal.copy(
        name = Name.text,
        coords = Coordinates(RA.toRightAscension, Dec.value),
        epoch = Epoch.selection.item,
        properMotion = PMCheck.selected match {
          case false => None
          case true  => Some(ProperMotion(DeltaRA.text.toDouble, DeltaDec.text.toDouble))
        },
        magnitudes = Tabs.magControls.flatMap(_.magnitude))

    case NonSiderealType =>

      import Header.Name
      import Tabs.EphemerisContent._

      nonSidereal.copy(
        name = Name.text,
        epoch = NsEpoch.selection.item,
        ephemeris = Viewer.getModel)

    case TooType =>

      import Header.Name
      too.copy(name = Name.text)

  }

}

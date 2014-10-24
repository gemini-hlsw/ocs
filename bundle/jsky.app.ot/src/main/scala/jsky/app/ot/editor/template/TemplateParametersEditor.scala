package jsky.app.ot.editor.template

import edu.gemini.pot.sp.ISPTemplateParameters
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.util.TimeValue
import edu.gemini.shared.util.immutable.DefaultImList
import edu.gemini.spModel.`type`.ObsoletableSpType
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{PercentageContainer, ImageQuality, CloudCover, SkyBackground, WaterVapor}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system._
import edu.gemini.spModel.template.TemplateParameters

import javax.swing.BorderFactory

import scala.collection.JavaConverters._
import scala.swing.event.{ButtonClicked, EditDone, SelectionChanged}
import scala.swing._
import scala.swing.ListView.Renderer
import scala.swing.GridBagPanel.Anchor.{East, North}
import scala.swing.GridBagPanel.Fill.Horizontal

import scalaz._

// Editor for staff-use to modify TemplateParameters.  Allows multi-selection
// and bulk editing to change many phase 1 observations at once.  For example,
// to change the observing conditions for all the observations.

object TemplateParametersEditor {
  val HGap = 5
  val VGap = 3

  // Get the common value across all the parameters, if any.
  def common[A](params: Iterable[TemplateParameters])(get: TemplateParameters => A): Option[A] = {
    val s = params.map(get).toSet
    if (s.size == 1) s.headOption else None
  }

  trait Initializable {
    def init(ps: Iterable[TemplateParameters]): Unit
  }

  case class Row(components: Component*) extends Initializable {

    def update(f: Component => Unit): Unit = components.foreach(f)

    def visible: Boolean = components.forall(_.visible)

    def visible_=(v: Boolean): Unit =
      update(_.visible = v)

    def init(ps: Iterable[TemplateParameters]): Unit =
      components.foreach {
        case ini: Initializable => ini.init(ps)
        case _                  => // skip
      }
  }

  object Row {
    def apply(label: String, components: Component*): Row = {
      val l = new Label(label) { horizontalAlignment = Alignment.Right }
      new Row(l :: components.toList: _*)
    }
  }

  abstract class ColumnPanel(hGap: Int = HGap, rowAnchor: GridBagPanel.Anchor.Value = East) extends GridBagPanel with Initializable {
    def rows: Iterable[Row]

    def nextY(): Int = (-1 :: layout.values.toList.map(_.gridy)).max + 1

    def layoutRow(r: Row): Unit = {
      val y    = nextY()
      val size = r.components.length

      r.components.zipWithIndex.foreach { case (c, i) =>
        layout(c) = new Constraints {
          gridx  = i
          gridy  = y
          anchor = rowAnchor
          insets = new Insets(0, if (i == 0) 0 else hGap, VGap, 0)
          fill   = Horizontal
        }
      }
    }

    def layoutRows(): Unit = rows.foreach(layoutRow)

    def init(ps: Iterable[TemplateParameters]): Unit = rows.foreach { _.init(ps) }
  }
}


import TemplateParametersEditor._


/**
 * An editor for TemplateParameters.
 */
class TemplateParametersEditor(shells: java.util.List[ISPTemplateParameters]) extends ColumnPanel(20, North) {
  border = BorderFactory.createEmptyBorder(10,10,10,10)

  // Fetches the current TemplateParameters from the shells.
  private def load: Iterable[TemplateParameters] =
    shells.asScala.map(_.getDataObject.asInstanceOf[TemplateParameters]).toIterable

  // Applies the given function to a copy of the current template parameters
  // data object and writes the update back to the shells.
  private def store(up: TemplateParameters => TemplateParameters): Unit =
    shells.asScala.foreach { shell =>
      shell.setDataObject(up(shell.getDataObject.asInstanceOf[TemplateParameters]))
    }

  trait BoundWidget[A] extends Initializable { self: Component =>
    def get: TemplateParameters => A
    def set: (TemplateParameters, A) => TemplateParameters

    def updateEnabledState(): Unit = enabled = shells.size > 0
    def reinit(): Unit = init(load)

    def displayedValue: Option[A]

    // Write the currently displayed value back into TemplateParameters and then
    // update the shells with the new TemplateParameters.
    def writeThrough(): Unit =
      displayedValue.fold(reinit()) { a =>
        if (common(load)(get).forall(_ != a)) store { set(_, a) }
      }
  }

  class BoundTextField[A](cols: Int)(
      read: String => A,
      show: A => String,
      val get: TemplateParameters => A,
      val set: (TemplateParameters, A) => TemplateParameters) extends TextField(cols) with BoundWidget[A] {

    def displayedValue: Option[A] = \/.fromTryCatch(read(text)).toOption

    val reaction: Reactions.Reaction = { case EditDone(_) => writeThrough() }

    def init(ps: Iterable[TemplateParameters]): Unit = {
      reactions -= reaction
      text = common(ps)(get).fold("") { show }
      reactions += reaction
      updateEnabledState()
    }

    listenTo(this)
  }

  // A combo box with the given options, but capable of rendering a null
  // selection (which is used to signify that one ore more different template
  // parameters have a different value for this element)
  class BoundNullableCombo[A >: Null](opts: Seq[A])(
      show: A => String,
      val get: TemplateParameters => A,
      val set: (TemplateParameters, A) => TemplateParameters) extends ComboBox(opts) with BoundWidget[A] {

    // Show a blank when there isn't a common value across all params.
    renderer = Renderer { maybeNull => Option(maybeNull).fold("") { show } }

    def displayedValue: Option[A] = Option(selection.item)

    val reaction: Reactions.Reaction = { case SelectionChanged(_) => writeThrough() }

    def init(ps: Iterable[TemplateParameters]): Unit = {
      selection.reactions -= reaction
      selection.item = common(ps)(get).orNull
      selection.reactions += reaction
      updateEnabledState()
    }
  }

  class BoundCheckbox(
    val get: TemplateParameters => Boolean,
    val set: (TemplateParameters, Boolean) => TemplateParameters) extends CheckBox with BoundWidget[Boolean] {

    def displayedValue: Option[Boolean] = Some(selected)

    val reaction: Reactions.Reaction = { case ButtonClicked(_) => writeThrough() }

    def init(ps: Iterable[TemplateParameters]): Unit = {
      reactions -= reaction
      selected = common(ps)(get).getOrElse(false)
      reactions += reaction
      updateEnabledState()
    }

    listenTo(this)
  }


  object TargetPanel extends ColumnPanel(10, North) {
    trait TargetType { def display: String}
    case object Sidereal extends TargetType    { val display = "Sidereal"     }
    case object NonSidereal extends TargetType { val display = "Non-Sidereal" }

    private val AllTargetTypes = List(Sidereal, NonSidereal)

    def setTarget[A](up: (SPTarget, A) => Unit)(tp: TemplateParameters, a: A): TemplateParameters = {
      val newTarget = tp.getTarget
      up(newTarget, a)
      tp.copy(newTarget)
    }

    def targetType(t: SPTarget): TargetType =
      if (t.getTarget.isInstanceOf[NonSiderealTarget]) NonSidereal else Sidereal

    object CoordinatesPanel extends ColumnPanel {
      val nameField = new BoundTextField[String](10)(
        read = identity,
        show = identity,
        get  = _.getTarget.getName,
        set  = setTarget(_.setName(_))
      )

      val typeCombo = new BoundNullableCombo[TargetType](AllTargetTypes)(
        show = _.display,
        get  = { tp => targetType(tp.getTarget) },
        set  = { setTarget((target, targetType) => {
          val coords = targetType match {
            case Sidereal    => new HmsDegTarget()
            case NonSidereal => new ConicTarget()
          }
          coords.setC1(new HMS(target.getXaxis))
          coords.setC2(new DMS(target.getYaxis))
          coords.setName(target.getName)
          target.setTarget(coords)
        })}
      )

      val hms = new HMSFormat()
      val raField = new BoundTextField[HMS](10)(
        read = s => new HMS(hms.parse(s)),
        show = _.toString,
        get  = _.getTarget.getTarget.getTargetAsJ2000.getC1.asInstanceOf[HMS],
        set  = setTarget(_.getTarget.setC1(_))
      )

      val dms = new DMSFormat()
      val decField = new BoundTextField[DMS](10)(
        read = s => new DMS(dms.parse(s)),
        show = _.toString,
        get  = _.getTarget.getTarget.getTargetAsJ2000.getC2.asInstanceOf[DMS],
        set  = setTarget(_.getTarget.setC2(_))
      )

      def pmField(get: SPTarget => String, set: (SPTarget, String) => Unit): BoundTextField[Double] =
        new BoundTextField[Double](10)(
          read = _.toDouble,
          show = _.toString,
          get  = tp => get(tp.getTarget).toDouble,
          set  = (tp, pm) => {
            val newTarget = tp.getTarget
            set(newTarget, pm.toString)
            tp.copy(newTarget)
          }
        )


      val pmRaRow  = Row("Delta RA",  pmField(_.getPropMotionRA,  _.setPropMotionRA(_)))
      val pmDecRow = Row("Delta Dec", pmField(_.getPropMotionDec, _.setPropMotionDec(_)))

      val rows = List(
        Row("Name",      nameField ),
        Row("Type",      typeCombo ),
        Row("RA",        raField   ),
        Row("Dec",       decField  ),
        pmRaRow,
        pmDecRow
      )

      override def init(ps: Iterable[TemplateParameters]): Unit = {
        super.init(ps)
        showTypeSpecificWidgets(ps)
      }

      def showTypeSpecificWidgets(): Unit = showTypeSpecificWidgets(load)

      def showTypeSpecificWidgets(ps: Iterable[TemplateParameters]): Unit = {
        val typeOpt = common(ps)(tp => targetType(tp.getTarget))
        val isSid   = typeOpt.exists(_ == Sidereal)
        pmRaRow.visible  = isSid
        pmDecRow.visible = isSid

        revalidate()
        repaint()
      }

      layoutRows()

      // When the target type changes, we have to update the display to match.
      // Here we listen to the same event that actually stores the changes, so
      // make sure the update happens after the changes are stored. This is
      // pretty awful.  We could watch the shells for data object updates
      // instead I suppose.
      listenTo(typeCombo.selection)
      reactions += { case SelectionChanged(_) => Swing.onEDT(showTypeSpecificWidgets()) }
    }

    object MagnitudesPanel extends ColumnPanel {
      val band = Magnitude.Band.B
      lazy val zero = new Magnitude(band, 0.0)

      val magCheck = new BoundCheckbox(
        get = !_.getTarget.getMagnitude(band).isEmpty,
        set = setTarget((target, inc) => {
          if (inc) target.putMagnitude(zero)
          else {
            val mags = target.getMagnitudes.toList.asScala.filterNot(_.getBand == band)
            target.setMagnitudes(DefaultImList.create(mags.asJava))
          }}
        )
      )

      val magValue = new BoundTextField[Double](4)(
        read = _.toDouble,
        show = _.toString,
        get  = _.getTarget.getMagnitude(band).getOrElse(zero).getBrightness,
        set  = setTarget((t, b) => t.putMagnitude(new Magnitude(band, b)))
      ) {
        listenTo(magCheck)
        reactions += {
          case ButtonClicked(`magCheck`) => reinit()
        }

        override def updateEnabledState(): Unit = {
          enabled = shells.size() > 0 && magCheck.selected
        }
      }

      val rows = List(
        Row(magCheck, new Label("B"), magValue)
      )
      layoutRows()
    }

    val rows = List(
      Row(CoordinatesPanel, MagnitudesPanel)
    )
    layoutRows()
  }


  object ConditionsAndTimePanel extends ColumnPanel {
    border = BorderFactory.createEmptyBorder(0, 50, 0, 50)

    private def mkCombo[A >: Null <: PercentageContainer](opts: Seq[A])(get: SPSiteQuality => A, set: (SPSiteQuality, A) => Unit): BoundNullableCombo[A] = {
      // Filter out any obsolete values.
       val validOpts = opts.filter {
        case o: ObsoletableSpType => !o.isObsolete
        case _                    => true
      }

      new BoundNullableCombo[A](validOpts)(
        show = _.getPercentage match {
          case 100 => "Any"
          case p   => p.toString
        },
        get  = tp => get(tp.getSiteQuality),
        set  = (tp, a) => {
          val sq = tp.getSiteQuality
          set(sq, a)
          tp.copy(sq)
        }
      )
    }

    val timeField = new BoundTextField[Double](4)(
      read = _.toDouble,
      show = timeAmount => f"$timeAmount%.2f",
      get  = _.getTime.getTimeAmount,
      set  = (tp, timeAmount) => tp.copy(new TimeValue(timeAmount, tp.getTime.getTimeUnits))
    )

    import TimeValue.Units._
    val unitsCombo = new BoundNullableCombo[TimeValue.Units](Seq(hours, nights))(
      show = _.name,
      get  = _.getTime.getTimeUnits,
      set  = (tp, units) => tp.copy(new TimeValue(tp.getTime.getTimeAmount, units))
    )

    val rows = List(
      Row("Cloud Cover",    mkCombo(CloudCover.values   )(_.getCloudCover,    _.setCloudCover(_)   )),
      Row("Image Quality",  mkCombo(ImageQuality.values )(_.getImageQuality,  _.setImageQuality(_) )),
      Row("Sky Background", mkCombo(SkyBackground.values)(_.getSkyBackground, _.setSkyBackground(_))),
      Row("Water Vapor",    mkCombo(WaterVapor.values   )(_.getWaterVapor,    _.setWaterVapor(_)   )),
      Row(" "),
      Row("Phase 1 Time",   timeField, unitsCombo)
    )
    layoutRows()
  }

  val rows = List(
    Row(TargetPanel, ConditionsAndTimePanel)
  )

  layoutRows()
  init(load)
}
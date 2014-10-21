package jsky.app.ot.editor.template

import edu.gemini.pot.sp.ISPTemplateParameters
import edu.gemini.shared.util.TimeValue
import edu.gemini.spModel.`type`.ObsoletableSpType
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{PercentageContainer, ImageQuality, CloudCover, SkyBackground, WaterVapor}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system._
import edu.gemini.spModel.template.TemplateParameters

import javax.swing.BorderFactory

import scala.collection.JavaConverters._
import scala.swing.event.{EditDone, SelectionChanged}
import scala.swing._
import scala.swing.ListView.Renderer
import scala.swing.GridBagPanel.Anchor.East
import scala.swing.GridBagPanel.Fill.Horizontal

import scalaz._

object TemplateParametersEditor {
  val LabelGap = 5
  val VGap     = 3

  // Get the common value across all the parameters, if any.
  def common[A](params: Iterable[TemplateParameters])(get: TemplateParameters => A): Option[A] = {
    val s = params.map(get).toSet
    if (s.size == 1) s.headOption else None
  }
}

import TemplateParametersEditor._

/**
 * An editor for TemplateParameters.
 */
class TemplateParametersEditor(shells: java.util.List[ISPTemplateParameters]) extends GridBagPanel {
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

  trait InitWidget extends Component {
    def init(ps: Iterable[TemplateParameters]): Unit
  }

  trait InitWidgetContainer extends InitWidget {
    def containedWidgets: Iterable[InitWidget]

    def init(ps: Iterable[TemplateParameters]): Unit =
      containedWidgets.foreach(_.init(ps))
  }

  case class Row(label: String, widget: InitWidget, units: Option[InitWidget] = None)

  trait ColumnPanel extends InitWidgetContainer { self: GridBagPanel =>
    private val LabelInsets  = new Insets(0, 0, VGap, LabelGap)
    private val WidgetInsets = new Insets(0, 0, VGap, 0)
    private val UnitsInsets  = new Insets(0, LabelGap, VGap, 0)

    def nextY(): Int = (-1 :: layout.values.toList.map(_.gridy)).max + 1

    def layoutRow(r: Row): Unit = layoutRow(r.label, r.widget, r.units)

    def layoutRow(text: String, c: Component, units: Option[Component] = None): Unit = {
      val y = nextY()
      layout(new Label(text)) = new Constraints {
        gridx  = 0
        gridy  = y
        anchor = East
        insets = LabelInsets
      }
      layout(c) = new Constraints {
        gridx  = 1
        gridy  = y
        fill   = Horizontal
        insets = WidgetInsets
      }
      units.foreach { u =>
        layout(u) = new Constraints {
          gridx  = 2
          gridy  = y
          fill   = Horizontal
          insets = UnitsInsets
        }
      }
    }

//    def pushUp(): Unit =
//      layout(new BorderPanel) = new Constraints {
//        gridy   = nextY()
//        weighty = 1.0
//        fill    = GridBagPanel.Fill.Both
//      }

    def layoutRows(): Unit = rows.foreach(layoutRow)

    def rows: Iterable[Row]

    def containedWidgets: Iterable[InitWidget] =
      rows.flatMap { r => r.widget :: r.units.toList }
  }


  class BoundTextField[A](cols: Int)(
      read: String => A,
      show: A => String,
      get: TemplateParameters => A,
      set: (TemplateParameters, A) => TemplateParameters) extends TextField(cols) with InitWidget {

    def writeThrough(): Unit =
      \/.fromTryCatch(read(text)).toOption.fold(init(load)) { a =>
        if (common(load)(get).forall(_ != a))
          store { set(_, a) }
      }

    val reaction: Reactions.Reaction = { case EditDone(_) => writeThrough() }

    def init(ps: Iterable[TemplateParameters]): Unit = {
      reactions -= reaction
      text = common(ps)(get).fold("") { show }
      reactions += reaction
    }

    listenTo(this)
  }

  // A combo box with the given options, but capable of rendering a null
  // selection (which is used to signify that one ore more different template
  // parameters have a different value for this element)
  class BoundNullableCombo[A >: Null](opts: Seq[A])(
      show: A => String,
      get: TemplateParameters => A,
      set: (TemplateParameters, A) => TemplateParameters) extends ComboBox(opts) with InitWidget {

    // Show a blank when there isn't a common value across all params.
    renderer = Renderer { maybeNull => Option(maybeNull).fold("") { show } }

    // Gets the updated selection from the combo box, assuming it is defined
    // and differs from the current common value.
    private def selectionUpdate: Option[A] =
      Option(selection.item).filterNot { sel =>
        common(load)(get).exists(_ == sel)
      }

    def writeThrough(): Unit =
      selectionUpdate.foreach { a => store { set(_, a) } }

    val reaction: Reactions.Reaction = { case SelectionChanged(_) => writeThrough() }

    def init(ps: Iterable[TemplateParameters]): Unit = {
      selection.reactions -= reaction
      selection.item = common(ps)(get).orNull
      selection.reactions += reaction
    }
  }


  object TargetPanel extends GridBagPanel with InitWidgetContainer {
    // TODO: There's currently no way to distinguish a real sidereal target at
    // (0,0) from an ToO target.  This may be fixed when SpTarget is upgraded
    // to contain a spModel.core target.  For now, ToO == Sidereal.
    trait TargetType { def display: String}
    case object Sidereal extends TargetType    { val display = "Sidereal"     }
    case object NonSidereal extends TargetType { val display = "Non-Sidereal" }

    private val AllTargetTypes = List(Sidereal, NonSidereal)

    def setTarget[A](up: (SPTarget, A) => Unit)(tp: TemplateParameters, a: A): TemplateParameters = {
      val newTarget = tp.getTarget
      up(newTarget, a)
      tp.copy(newTarget)
    }

    object CoordinatesPanel extends GridBagPanel with ColumnPanel {
      val nameField = new BoundTextField[String](10)(
        read = identity,
        show = identity,
        get  = _.getTarget.getName,
        set  = setTarget(_.setName(_))
      )

      def targetType(t: SPTarget): TargetType =
        if (t.getTarget.isInstanceOf[NonSiderealTarget]) NonSidereal else Sidereal

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

//      val pmRaField = new BoundTextField[Double](10)(
//        read = _.toDouble,
//        show = _.toString,
//        get  = _.getTarget.getPropMotionRA.toDouble,
//        set  = (tp, pm) => {
//          val newTarget = tp.getTarget
//          newTarget.setPropMotionRA(pm.toString)
//          tp.copy(newTarget)
//        }
//      )

      val rows = List(
        Row("Name", nameField),
        Row("Type", typeCombo),
        Row("RA",   raField),
        Row("Dec",  decField)
      )

      layoutRows()
    }

    layout(CoordinatesPanel) = new Constraints {
      gridx   = 0
      gridy   = 0
    }

    def containedWidgets: Iterable[InitWidget] = List(CoordinatesPanel)
  }


  object ConditionsPanel extends GridBagPanel with ColumnPanel {
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

    val rows = List(
      Row("CC", mkCombo(CloudCover.values   )(_.getCloudCover,    _.setCloudCover(_)   )),
      Row("IQ", mkCombo(ImageQuality.values )(_.getImageQuality,  _.setImageQuality(_) )),
      Row("SB", mkCombo(SkyBackground.values)(_.getSkyBackground, _.setSkyBackground(_))),
      Row("WV", mkCombo(WaterVapor.values   )(_.getWaterVapor,    _.setWaterVapor(_)   ))
    )

    layoutRows()
  }

  object TimePanel extends GridBagPanel with ColumnPanel {
    val timeField = new BoundTextField[Double](5)(
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
      Row("Time", timeField, Some(unitsCombo))
    )
    layoutRows()
  }

  {
    def layoutBorder(x: Int): Unit =
      layout(new BorderPanel) = new Constraints {
        gridx   = x
        weightx = 1.0
        fill    = Horizontal
      }

    def layoutContent(p: Panel, x: Int): Unit =
      layout(p) = new Constraints {
        gridx   = x
        weighty = 1.0
        fill    = GridBagPanel.Fill.None
        anchor  = GridBagPanel.Anchor.North
      }

    layoutBorder(0)

    val params = load
    val pans   = List(TargetPanel, ConditionsPanel, TimePanel)
    pans.zipWithIndex.foreach {
      case (p, x) =>
        layoutContent(p, x + 1)
        p.init(params)
    }

    layoutBorder(pans.length + 1)
  }
}
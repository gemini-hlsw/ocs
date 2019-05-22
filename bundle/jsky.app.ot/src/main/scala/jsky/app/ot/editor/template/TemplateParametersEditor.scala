package jsky.app.ot.editor.template

import edu.gemini.pot.sp.ISPTemplateParameters
import edu.gemini.shared.util.TimeValue
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption}
import edu.gemini.spModel.`type`.ObsoletableSpType
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, ImageQuality, PercentageContainer, SkyBackground, WaterVapor}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.template.TemplateParameters
import javax.swing.BorderFactory
import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.text.Document

import scala.collection.JavaConverters._
import scala.swing.ScrollPane.BarPolicy
import scala.swing.event.{ButtonClicked, EditDone, SelectionChanged}
import scala.swing._
import scala.swing.ListView.Renderer
import scala.swing.GridBagPanel.Anchor.{East, North}
import scala.swing.GridBagPanel.Fill.{Horizontal, Vertical}
import scalaz._
import Scalaz._
import edu.gemini.spModel.too.Too

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

  sealed trait Initializable {
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
      val y = nextY()

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
 * An editor for TemplateParameters.  The editor is recreated with the currently
 * selected collection of template parameters.  It shows the common value across
 * all selected parameters or a blank if the values differ.  Setting the value
 * in a widget will update the value for all selected parameters.
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

  private def isToo: Boolean =
    shells.asScala.exists(Too.isToo(_))

  sealed trait BoundWidget[A] extends Initializable { self: Component =>
    def get: TemplateParameters => A
    def set: (TemplateParameters, A) => TemplateParameters

    def updateEnabledState(): Unit = enabled = shells.size > 0
    def reinit(): Unit = init(load)

    def displayedValue: Option[A]

    // Write the currently displayed value back into TemplateParameters and then
    // update the shells with the new TemplateParameters.
    def writeThrough(): Unit =
      displayedValue.foreach { a =>
        if (common(load)(get).forall(_ != a)) store { set(_, a) }
      }
  }

  class BoundTextField[A](cols: Int)(
      read: String => A,
      show: A => String,
      val get: TemplateParameters => A,
      val set: (TemplateParameters, A) => TemplateParameters) extends TextField(cols) with BoundWidget[A] {

    private def doc: Document   = peer.getDocument
    private def readDoc: String = doc.getText(0, doc.getLength)

    private val docListener = new DocumentListener {
      override def insertUpdate(e: DocumentEvent): Unit  = changedUpdate(e)
      override def removeUpdate(e: DocumentEvent): Unit  = changedUpdate(e)
      override def changedUpdate(e: DocumentEvent): Unit = writeThrough()
    }

    private def listenToDoc(): Unit = doc.addDocumentListener(docListener)
    private def deafToDoc(): Unit   = doc.removeDocumentListener(docListener)

    def displayedValue: Option[A] = \/.fromTryCatchNonFatal(read(readDoc)).toOption

    private def showCommonValue(ps: Iterable[TemplateParameters]): Unit = {
      deafToDoc()
      text = common(ps)(get).fold("") { show }
      listenToDoc()
      caret.position = 0
    }

    def init(ps: Iterable[TemplateParameters]): Unit = {
      showCommonValue(ps)
      updateEnabledState()
    }

    // Watch the underlying doc (as opposed to the EditDone event) to record
    // updates as they happen as in the rest of the OT and to not try to record
    // any updates _unless_ they happen.
    listenToDoc()

    // Watch for EditDone just to show the common value properly formatted when
    // editing finishes.
    reactions += { case EditDone(_) => showCommonValue(load) }
    listenTo(this)
  }

  // A combo box with the given options, but capable of rendering a null
  // selection (which is used to signify that one or more different template
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

  object TargetPanel extends GridBagPanel with Initializable {
    sealed trait TargetType { def display: String }
    case object Sidereal extends TargetType    { val display = "Sidereal"     }
    case object NonSidereal extends TargetType { val display = "Non-Sidereal" }
    case object ToO extends TargetType { val display = "Target of Opportunity" }

    private val AllTargetTypes = List(Sidereal, NonSidereal) ++ (if (isToo) List(ToO) else Nil)

    def setTarget[A](up: (SPTarget, A) => Unit)(tp: TemplateParameters, a: A): TemplateParameters = {
      val newTarget = tp.getTarget
      up(newTarget, a)
      tp.copy(newTarget)
    }

    def targetType(t: SPTarget): TargetType = t.getTarget match {
      case _: SiderealTarget    => Sidereal
      case _: NonSiderealTarget => NonSidereal
      case _: TooTarget         => ToO
    }

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
          targetType match {
            case ToO         => target.setTOO()
            case Sidereal    => target.setSidereal()
            case NonSidereal => target.setNonSidereal()
          }
          target.setName(target.getName)
          target.setMagnitudes(Nil)
        })}
      )

      val JNoneLong: JOption[java.lang.Long] = JNone.instance[java.lang.Long]

      def updateCoordinate[A](lens: Target @?> A): (TemplateParameters, A) => TemplateParameters =
        setTarget((a, b) => lens.set(a.getTarget, b).foreach(a.setTarget))

      val raField = new BoundTextField[RightAscension](10)(
        read = s => Angle.parseHMS(s).map(RightAscension.fromAngle).valueOr(ex => throw ex),
        show = _.toAngle.formatHMS,
        get  = _.getTarget.getCoordinates(None).map(_.ra) | RightAscension.zero,
        set  = updateCoordinate(Target.ra)
      )

      val decField = new BoundTextField[Declination](10)(
        read = s => Angle.parseDMS(s).flatMap(a => Declination.fromAngle(a) \/> new IllegalArgumentException(s"$s is not a valid declination")).valueOr(ex => throw ex),
        show = _.formatDMS,
        get  = _.getTarget.getCoordinates(None).map(_.dec) | Declination.zero,
        set  = updateCoordinate(Target.dec)
      )

      def pmField(lens: SiderealTarget @> Double): BoundTextField[Double] =
        new BoundTextField[Double](10)(
          read = _.toDouble,
          show = d => f"$d%.3f",
          get  = tp => tp.getTarget.getSiderealTarget.fold(0.0)(lens.get),
          set  = (tp, pm) => {
            val newTarget = tp.getTarget            // Get an SPTarget clone of the template param target
            newTarget.getSiderealTarget.foreach { t =>
              newTarget.setTarget(lens.set(t, pm))  // Set the cloned SPTarget's immutable Target after updating
            }
            tp.copy(newTarget)                      // Update the template parameter with the new SPTarget
          }
        )

      // Lens that mediates ProperMotion.zero ~ None ... not clear why we distinguish this in the
      // model when no user-facing code cares.
      val totalPM: SiderealTarget @> ProperMotion =
        SiderealTarget.properMotion.xmapB(_.getOrElse(ProperMotion.zero))(Some(_).filterNot(_ == ProperMotion.zero))

      // Given the lens above we can lens dRA and dDec
      val totalPMRA  = totalPM >=> ProperMotion.deltaRA  >=> RightAscensionAngularVelocity.velocity >=> AngularVelocity.masPerYear
      val totalPMDec = totalPM >=> ProperMotion.deltaDec >=> DeclinationAngularVelocity.velocity    >=> AngularVelocity.masPerYear

      val siderealRows = List(
        Row("RA",     raField),
        Row("Dec",    decField),
        Row("pm RA",  pmField(totalPMRA)),
        Row("pm Dec", pmField(totalPMDec))
      )

      val rows = List(
        Row("Name",   nameField ),
        Row("Type",   typeCombo )
      ) ++ siderealRows

      def showSiderealRows(v: Boolean): Unit =
        siderealRows.foreach(_.visible = v)

      layoutRows()
    }

    object MagnitudesPanel extends ColumnPanel {
      def magRow(band: MagnitudeBand): Row = {
        lazy val zero = new Magnitude(0.0, band, band.defaultSystem)

        def mag(tp: TemplateParameters): Option[Magnitude] =
          tp.getTarget.getMagnitude(band)

        def magOrZero(tp: TemplateParameters): Magnitude =
          mag(tp).getOrElse(zero)

        def setMag[A](f: (Magnitude, A) => Magnitude): (TemplateParameters, A) => TemplateParameters =
          setTarget[A]{ (t, a) =>
            t.putMagnitude(f(t.getMagnitude(band).getOrElse(zero), a))
          }

        val magCheck = new BoundCheckbox(
          get = mag(_).isDefined,
          set = setTarget((target, inc) => {
            if (inc) {
              target.putMagnitude(zero)
            } else {
              val mags = target.getMagnitudes.filterNot(_.band == band)
              target.setMagnitudes(mags)
            }}
          )
        )

        def includeBand: Boolean = shells.size > 0 && magCheck.selected

        val magValue = new BoundTextField[Double](5)(
          read = _.toDouble,
          show = d => f"$d%.3f",
          get  = magOrZero(_).value,
          set  = setMag((m, b) => new Magnitude(b, band, m.system))
        ) {
          override def updateEnabledState(): Unit = enabled = includeBand
        }

        val magSys = new BoundNullableCombo[MagnitudeSystem](MagnitudeSystem.allForOT)(
          show = _.name,
          get  = magOrZero(_).system,
          set  = setMag((m, s) => new Magnitude(m.value, band, s))
        ) {
          override def updateEnabledState(): Unit = enabled = includeBand
        }

        listenTo(magCheck)
        reactions += {
          case ButtonClicked(`magCheck`) =>
            magValue.reinit()
            magSys.reinit()
        }

        Row(band.name, magCheck, magValue, magSys)
      }

      val rows = MagnitudeBand.all.map(magRow)
      layoutRows()
    }

    val magScroll = new ScrollPane(MagnitudesPanel) {
      horizontalScrollBarPolicy = BarPolicy.Never
      verticalScrollBarPolicy   = BarPolicy.AsNeeded
      border = BorderFactory.createEmptyBorder(0,0,0,0)

      val dim = new Dimension(180, 1)
      maximumSize   = dim
      preferredSize = dim
      minimumSize   = dim
    }

    def init(ps: Iterable[TemplateParameters]): Unit = {
      CoordinatesPanel.init(ps)
      MagnitudesPanel.init(ps)
      showTypeSpecificWidgets(ps)
    }

    layout(CoordinatesPanel) = new Constraints {
      insets = new Insets(0, 0, VGap, 0)
      fill   = Horizontal
    }

    layout(magScroll) = new Constraints {
      gridx   = 1
      insets  = new Insets(0, HGap, VGap, 0)
      fill    = Vertical
      weightx = 1.0
    }

    def showTypeSpecificWidgets(ps: Iterable[TemplateParameters]): Unit = {
      val isSid = common(ps)(tp => targetType(tp.getTarget)).contains(Sidereal)
      CoordinatesPanel.showSiderealRows(isSid)
      magScroll.visible = isSid

      revalidate()
      repaint()
    }

    def reinitTypeSpecificWidgets(): Unit = {
      val ps = load
      CoordinatesPanel.siderealRows.foreach(_.init(ps))
      MagnitudesPanel.init(ps)
      showTypeSpecificWidgets(ps)
    }

    // When the target type changes, we have to update the display to match.
    // Here we listen to the same event that actually stores the changes, so
    // make sure the update happens after the changes are stored. This is
    // pretty awful.  We could watch the shells for data object updates
    // instead I suppose.
    listenTo(CoordinatesPanel.typeCombo.selection)
    reactions += { case SelectionChanged(_) => Swing.onEDT(reinitTypeSpecificWidgets()) }
  }


  object ConditionsAndTimePanel extends ColumnPanel {
    border = BorderFactory.createEmptyBorder(0, 25, 0, 0)

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
      Row("CC", mkCombo(CloudCover.values   )(_.getCloudCover,    _.setCloudCover(_)   )),
      Row("IQ", mkCombo(ImageQuality.values )(_.getImageQuality,  _.setImageQuality(_) )),
      Row("SB", mkCombo(SkyBackground.values)(_.getSkyBackground, _.setSkyBackground(_))),
      Row("WV", mkCombo(WaterVapor.values   )(_.getWaterVapor,    _.setWaterVapor(_)   )),
      Row(" "),
      Row("Time", timeField, unitsCombo)
    )
    layoutRows()
  }

  val rows = List(
    Row(TargetPanel, ConditionsAndTimePanel)
  )

  layoutRows()
  init(load)
}
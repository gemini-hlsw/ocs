package jsky.app.ot.editor.template

import javax.swing.BorderFactory

import edu.gemini.pot.sp.ISPTemplateParameters
import edu.gemini.shared.util.TimeValue
import edu.gemini.spModel.`type`.ObsoletableSpType
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{PercentageContainer, ImageQuality, CloudCover, SkyBackground, WaterVapor}
import edu.gemini.spModel.template.TemplateParameters

import scala.collection.JavaConverters._
import scala.swing.event.{EditDone, SelectionChanged}
import scala.swing._
import scala.swing.ListView.Renderer
import scala.swing.GridBagPanel.Anchor.East
import scala.swing.GridBagPanel.Fill.{Horizontal, Vertical}

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

  private def store(up: TemplateParameters => TemplateParameters): Unit =
    shells.asScala.foreach { shell =>
      val obj = shell.getDataObject.asInstanceOf[TemplateParameters]
      shell.setDataObject(up(obj))
    }

  trait InitWidget {
    def init(ps: Iterable[TemplateParameters]): Unit
  }

  trait InitWidgetContainer extends InitWidget {
    def containedWidgets: Iterable[InitWidget]

    def init(ps: Iterable[TemplateParameters]): Unit =
      containedWidgets.foreach(_.init(ps))
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


  object ConditionsPanel extends GridBagPanel with InitWidgetContainer {
    border = BorderFactory.createEmptyBorder(0, 20, 0, 20)

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

    private def addCombo(n: String, c: ComboBox[_], y: Int): Unit = {
      layout(new Label(n)) = new Constraints {
        gridx  = 0
        gridy  = y
        anchor = East
        insets = new Insets(0, 0, VGap, LabelGap)
      }
      layout(c) = new Constraints {
        gridx  = 1
        gridy  = y
        insets = new Insets(0, 0, VGap, 0)
        fill   = Horizontal
      }
    }

    val all = List(
      "CC" -> mkCombo(CloudCover.values   )(_.getCloudCover,    _.setCloudCover(_)   ),
      "IQ" -> mkCombo(ImageQuality.values )(_.getImageQuality,  _.setImageQuality(_) ),
      "SB" -> mkCombo(SkyBackground.values)(_.getSkyBackground, _.setSkyBackground(_)),
      "WV" -> mkCombo(WaterVapor.values   )(_.getWaterVapor,    _.setWaterVapor(_)   )
    )

    all.zipWithIndex.foreach { case ((n,c), y) => addCombo(n, c, y) }

    def containedWidgets: Iterable[InitWidget] = all.map(_._2)
  }

  object TimePanel extends GridBagPanel with InitWidgetContainer {
    layout(new Label("Time")) = new Constraints {
      gridx  = 0
      insets = new Insets(0, 0, 0, LabelGap)
    }

    val timeField = new BoundTextField[Double](5)(
      read = _.toDouble,
      show = timeAmount => f"$timeAmount%.2f",
      get  = _.getTime.getTimeAmount,
      set  = (tp, timeAmount) => tp.copy(new TimeValue(timeAmount, tp.getTime.getTimeUnits))
    )

    layout(timeField) = new Constraints {
      gridx  = 1
      insets = new Insets(0, 0, 0, LabelGap)
    }

    import TimeValue.Units._
    val unitsCombo = new BoundNullableCombo[TimeValue.Units](Seq(hours, nights))(
      show = _.name,
      get  = _.getTime.getTimeUnits,
      set  = (tp, units) => tp.copy(new TimeValue(tp.getTime.getTimeAmount, units))
    )

    layout(unitsCombo) = new Constraints {
      gridx = 2
    }

    // push everything up
    layout(new BorderPanel) = new Constraints {
      gridx     = 0
      gridy     = 1
      weighty   = 1.0
      gridwidth = 3
      fill      = Vertical
    }

    def containedWidgets: Iterable[InitWidget] = List(timeField, unitsCombo)
  }

  layout(ConditionsPanel) = new Constraints {
    gridx   = 1
    weighty = 1.0
    fill    = Vertical
  }

  layout(TimePanel) = new Constraints {
    gridx   = 2
    weighty = 1.0
    fill    = Vertical
  }

  // push everything to the left
  layout(new BorderPanel) = new Constraints {
    gridx   = 3
    weightx = 1.0
    fill    = Horizontal
  }

  {
    val ps = load
    List(ConditionsPanel, TimePanel).foreach(_.init(ps))
  }
}


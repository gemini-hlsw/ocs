package jsky.app.ot.editor.template

import javax.swing.BorderFactory

import edu.gemini.pot.sp.ISPTemplateParameters
import edu.gemini.spModel.`type`.ObsoletableSpType
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{PercentageContainer, ImageQuality, CloudCover, SkyBackground, WaterVapor}
import edu.gemini.spModel.template.TemplateParameters

import scala.collection.JavaConverters._
import scala.swing.event.SelectionChanged
import scala.swing.{Insets, Label, ComboBox, GridBagPanel}
import scala.swing.ListView.Renderer
import scala.swing.GridBagPanel.Anchor.East
import scala.swing.GridBagPanel.Fill.{Both, Horizontal}

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

  def params: Iterable[TemplateParameters] =
    shells.asScala.map(_.getDataObject.asInstanceOf[TemplateParameters]).toIterable

  object ConditionsPanel extends GridBagPanel {
    case class Combo[A <: PercentageContainer](name: String, opts: Seq[A])(get: SPSiteQuality => A, set: (SPSiteQuality, A) => Unit)(implicit ev: Null <:< A) {

      // Filter out any obsolete values.
      def validOpts = opts.filter {
        case o: ObsoletableSpType => !o.isObsolete
        case _                    => true
      }

      // A combo box with the given options, but capable of rendering a null
      // selection (which is used to signify that one ore more different
      // template parameters have a different value for this conditions element)
      val comboBox = new ComboBox(validOpts) {
        renderer = Renderer(maybeNull =>
          Option(maybeNull).map { perc => perc.getPercentage match {
            case 100 => "Any"
            case p   => p.toString
          }}.getOrElse(""))
      }

      def commonConditions(ps: Iterable[TemplateParameters]): Option[A] =
        common(ps) { tp => get(tp.getSiteQuality) }

      // Gets the updated selection from the combo box, assuming it is defined
      // and differs from the current common value
      private def selectionUpdate: Option[A] =
        Option(comboBox.selection.item).filterNot { sel =>
          commonConditions(params).exists(_ == sel)
        }

      // When the user picks a value from the combo, store the changes in the
      // SPSiteQuality and write through to the TemplateParameters data obj.
      comboBox.selection.reactions += {
        case SelectionChanged(`comboBox`) =>
          selectionUpdate.foreach { a =>
            shells.asScala.foreach { shell =>
              val obj = shell.getDataObject.asInstanceOf[TemplateParameters]
              val sq  = obj.getSiteQuality
              set(sq, a)
              shell.setDataObject(obj.copy(sq))
            }
          }
      }

      def init(ps: Iterable[TemplateParameters]): Unit =
        comboBox.selection.item = commonConditions(ps).orNull
    }

    private def addCombo(c: Combo[_], y: Int): Unit = {
      layout(new Label(c.name)) = new Constraints {
        gridx  = 0
        gridy  = y
        anchor = East
        insets = new Insets(0, 0, VGap, LabelGap)
      }
      layout(c.comboBox) = new Constraints {
        gridx  = 1
        gridy  = y
        insets = new Insets(0, 0, VGap, 0)
        fill   = Horizontal
      }
    }

    val all = List(
      Combo("CC", CloudCover.values)(_.getCloudCover, _.setCloudCover(_)),
      Combo("IQ", ImageQuality.values)(_.getImageQuality, _.setImageQuality(_)),
      Combo("SB", SkyBackground.values)(_.getSkyBackground, _.setSkyBackground(_)),
      Combo("WV", WaterVapor.values)(_.getWaterVapor, _.setWaterVapor(_))
    )

    all.zipWithIndex.foreach { case (c, y) => addCombo(c, y) }

    def init(ps: Iterable[TemplateParameters]): Unit = all.foreach(_.init(ps))
  }

  layout(ConditionsPanel) = new Constraints {
    gridx   = 0
    gridy   = 0
    weightx = 1.0
    weighty = 1.0
    fill    = Both
  }

  val ps = params
  ConditionsPanel.init(ps)
}


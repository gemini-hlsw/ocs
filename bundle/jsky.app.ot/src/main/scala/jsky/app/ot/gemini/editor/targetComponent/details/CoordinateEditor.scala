package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system._
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.TextBoxWidget

import scala.util.Try
import scalaz.syntax.id._

// RA and Dec
class CoordinateEditor extends TelescopePosEditor with ReentrancyHack {

  private[this] var spt = new SPTarget

  val ra, dec = new TextBoxWidget <| {w =>
    w.setColumns(10)
    w.setMinimumSize(w.getPreferredSize)
  }

    ra.addWatcher(watcher { s =>
      nonreentrant {
        try {
          spt.setRaString(clean(s))
        } catch {
          case _: IllegalArgumentException => spt.setRaDegrees(0)
        }
      }
    })

  dec.addWatcher(watcher { s =>
    nonreentrant {
      clean(s) match {
        case "-" | "+" => // nop
        case s =>
          try {
            spt.setDecString(s)
          } catch {
            case _: IllegalArgumentException =>
              spt.setDecDegrees(0)
          }
      }
    }
  })

  def edit(ctx: GOption[ObsContext], target0: SPTarget, node: ISPNode): Unit = {
    val targetChanged = !target0.getTarget.equals(spt.getTarget)
    spt = target0

    nonreentrant {
      val when = ctx.asScalaOpt.flatMap(_.getSchedulingBlock.asScalaOpt).map(_.start).map(java.lang.Long.valueOf).asGeminiOpt

      // We want to see if the current text in the fields, when formatted properly, is different from what would be assigned.
      // The targetChanged is not a foolproof catch, since two ITargets could be considered the same if their values are
      // identical, but this is not really consequential: it is just included to reset a partially completed field to a
      // fully completed one (e.g. RA: 12 to 12:00:00.000) if the RAs are the same but the targets are different.
      def setField[F <: CoordinateFormat](widget: TextBoxWidget, formatter: F, extractor: GOption[java.lang.Long] => GOption[String]): Unit = {
        val original  = widget.getText
        val formatted = Try { formatter.format(formatter.parse(original)) }.getOrElse(original)
        extractor(when).asScalaOpt.filter(_ != formatted || original.isEmpty || targetChanged).foreach(widget.setText)
      }
      setField(ra,  HMS.DEFAULT_FORMAT, target.getRaString)
      setField(dec, DMS.DEFAULT_FORMAT, target.getDecString)
    }
  }

  def target: ITarget =
    spt.getTarget

  def clean(angle: String): String =
    angle.trim.replace(",", ".")

}

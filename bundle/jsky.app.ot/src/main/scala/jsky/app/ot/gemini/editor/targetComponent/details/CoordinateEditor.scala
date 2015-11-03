package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.{DMS, HMS, ITarget}
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
    val targetChanged = !target0.equals(spt)
    spt = target0

    nonreentrant {
      val when = ctx.asScalaOpt.flatMap(_.getSchedulingBlock.asScalaOpt).map(_.start).map(java.lang.Long.valueOf).asGeminiOpt

      // We want to see if the current text in the fields, when formatted properly, is different from what would be assigned.
      val raFormatted = Try { new HMS(ra.getText).toString }.getOrElse(ra.getText)
      target.getRaString(when).asScalaOpt.filter(_ != raFormatted || ra.getText.isEmpty || targetChanged).foreach(ra.setText)
      val decFormatted = Try { new DMS(dec.getText).toString }.getOrElse(dec.getText)
      target.getDecString(when).asScalaOpt.filter(_ != decFormatted || dec.getText.isEmpty || targetChanged).foreach(dec.setText)
    }
  }

  def target: ITarget =
    spt.getTarget

  def clean(angle: String): String =
    angle.trim.replace(",", ".")

}

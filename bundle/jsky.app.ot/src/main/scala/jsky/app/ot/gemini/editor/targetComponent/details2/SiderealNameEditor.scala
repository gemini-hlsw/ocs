package jsky.app.ot.gemini.editor.targetComponent.details2

import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.catalog.votable.{SimbadNameBackend, VoTableClient}
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.gui.GlassLabel
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.core.Target
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{ TextBoxWidgetWatcher, TextBoxWidget}
import jsky.util.gui.DialogUtil.{ error => errmsg }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.Swing
import scala.util.{ Success, Failure }

import scalaz.Scalaz._

final class SiderealNameEditor(mags: MagnitudeEditor2) extends TelescopePosEditor[SPTarget] with ReentrancyHack {
  private[this] var spt = new SPTarget // never null

  private def forkSearch(): Unit = {
    val searchItem = name.getValue
    val glass      = GlassLabel.show(name, "Searching ...") // We are on the EDT
    VoTableClient.catalog(CatalogQuery.nameSearch(searchItem), None)(implicitly).onComplete { case tqr =>
      Swing.onEDT {
        glass.foreach(_.hide())
        tqr.map(r => (r.result.problems, r.result.targets.rows.headOption)) match {
          case Failure(f)              => errmsg(f.getMessage)
          case Success((Nil, None))    => errmsg(s"Target '$searchItem' not found ")
          case Success((Nil, Some(t))) => spt.setTarget(Target.name.set(t, name.getValue)) // REL-2717
          case Success((ps, _))        => errmsg(ps.map(_.displayValue).mkString(", "))
        }
      }
    }
  }

  val name = new TextBoxWidget <| { w =>
    w.setColumns(20)
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {
      override def textBoxAction(tbwe: TextBoxWidget): Unit = forkSearch()
      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant(spt.setTarget(Target.name.set(spt.getTarget, tbwe.getValue)))
    })
  }

  val search = searchButton(forkSearch())

  def edit(ctx: GOption[ObsContext], target: SPTarget, node: ISPNode): Unit = {
    this.spt = target
    nonreentrant {
      name.setText(Target.name.get(spt.getTarget))
    }
  }

}

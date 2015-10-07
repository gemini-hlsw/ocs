package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.catalog.votable.{SimbadNameBackend, VoTableClient}
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.CoordinateTypes.{RV, Parallax}
import edu.gemini.spModel.target.system.HmsDegTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{DialogUtil, TextBoxWidgetWatcher, TextBoxWidget}
import edu.gemini.pot.ModelConverters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.Swing

import scalaz.syntax.id._
import scalaz.{ \/-, -\/ }

// Name editor, with catalog lookup for sidereal targets
final class SiderealNameEditor extends TelescopePosEditor with ReentrancyHack {
  // Why are we still using SPTarget here?
  private[this] var spt = new SPTarget // never null

  def forkSearch(): Unit = {
    val qf = VoTableClient.catalog(CatalogQuery(name.getValue), SimbadNameBackend)
    qf.onFailure {
      case f => DialogUtil.error(f.getMessage)
    }
    qf.onSuccess {
      case s if s.result.containsError => DialogUtil.error(s.result.problems.map(_.displayValue).mkString(", "))
      case s                           => processResult(s.result.targets.rows.headOption)
    }
  }

  val name = new TextBoxWidget <| { w =>
    w.setColumns(20)
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {

      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant {
          spt.setName(tbwe.getValue)
        }

      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        forkSearch()
    })
  }

  val search = searchButton(forkSearch())

  def edit(ctx: GOption[ObsContext], target: SPTarget, node: ISPNode): Unit = {
    this.spt = target
    nonreentrant {
      name.setText(spt.getTarget.getName)
    }
  }

  def processResult(target: Option[SiderealTarget]): Unit = Swing.onEDT {
    val t = spt.getTarget.asInstanceOf[HmsDegTarget]

    target.foreach { i =>
      i.properMotion.foreach { pm =>
        t.setPropMotionRA(pm.deltaRA.velocity.masPerYear)
        t.setPropMotionDec(pm.deltaDec.velocity.masPerYear)
      }
      // TODO: Should we pass the time?
      t.setRaString(i.coordinates.ra.toAngle.formatHMS)
      t.setDecString(i.coordinates.dec.formatDMS)

      i.parallax.foreach { p =>
        t.setParallax(new Parallax(p.mas))
      }
      i.radialVelocity.foreach {v =>
        t.setRV(new RV(v.velocity.toKilometersPerSecond))
      }
    }

    spt.notifyOfGenericUpdate()
  }

}

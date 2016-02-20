package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.Color
import java.util.Date
import javax.swing.JLabel

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ConicTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{ TextBoxWidget, TextBoxWidgetWatcher}

import Horizons.HorizonsIO

import scalaz._, Scalaz._

// N.B. in order to do a Horizons lookup, we need a way to get the "current" date, which is known
// to a different part of the editor. So we just pass in a program that knows how to do it.
final class ConicNameEditor(date: HorizonsIO[Date]) extends TelescopePosEditor with ReentrancyHack {

  private[this] var spt = new SPTarget // never null

  def ct: ConicTarget = spt.getConicTarget.get

  /** A program to resolve the current target BY NAME and replace it. */
  val lookup: HorizonsIO[Unit] =
    for {
      d  <- date
      t0 <- HorizonsIO.delay(ct)
      p  <- Horizons.lookupConicTargetByName(t0.getName, t0.getTag.unsafeToHorizonsObjectType, d)
      _  <- HorizonsIO.delay {
        spt.setTarget(p._1)
        spt.setSpatialProfile(ct.getSpatialProfile)
        spt.setSpectralDistribution(ct.getSpectralDistribution)
        spt.setMagnitudes(ct.getMagnitudes)
      }
    } yield ()

  val name = new TextBoxWidget <| { w =>
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {

      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant {
          spt.setName(tbwe.getValue)
          // editing the name so invalidate the horizons id.
          ct.setHorizonsObjectId(null) //
          ct.setHorizonsObjectTypeOrdinal(-1)
          hid.setText(hidText(ct))
          spt.notifyOfGenericUpdate()
        }

      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        lookup.invokeAndWait

    })
  }

  val search = searchButton(lookup.invokeAndWait)

  val hid = new JLabel <| { a =>
    a.setForeground(Color.DARK_GRAY)
  }

  def hidText(ct: ConicTarget): String =
    "Horizons ID: " + (ct.isHorizonsDataPopulated ?
            s"${ct.getHorizonsObjectId}/${ct.getHorizonsObjectType}" |
            "«unknown»")

  def edit(ctx: GOption[ObsContext], target: SPTarget, node: ISPNode): Unit = {
    this.spt = target
    nonreentrant {
      name.setText(ct.getName)
      hid.setText(hidText(ct))
    }
  }


}

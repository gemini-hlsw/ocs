package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.Color
import java.text.SimpleDateFormat
import java.util.{ Date, TimeZone }
import javax.swing.JLabel

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.core.{Declination, Angle, RightAscension, SiderealTarget, Coordinates, Target}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor

import scalaz._, Scalaz._

class EphemerisEditor extends TelescopePosEditor with ReentrancyHack {

  val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z") <| { df =>
    df.setTimeZone(TimeZone.getTimeZone("UTC"))
  }

  val start, end, size = new JLabel <| { l =>
    l.setForeground(Color.DARK_GRAY)
  }

  def clear(): Unit =
    List(start, end, size).foreach(_.setText("--"))

  def formatDate(ut: Long): String =
    df.format(new Date(ut))

  def edit(ctx: GOption[ObsContext], spt: SPTarget, node: ISPNode): Unit = {
    nonreentrant {
      Target.ephemeris.get(spt.getTarget) match {
        case None => clear()
        case Some(e) if e.isEmpty => clear()
        case Some(e) =>
          e.findMin.map(_._1).map(formatDate).foreach(s => start.setText(s + " –"))
          e.findMax.map(_._1).map(formatDate).foreach(end.setText)
          size.setText(e.size.toString + " elements")
      }
    }
  }


}

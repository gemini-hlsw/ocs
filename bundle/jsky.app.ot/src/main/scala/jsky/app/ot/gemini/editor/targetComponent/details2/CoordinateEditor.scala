package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.Color

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension, Target}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.{SPCoordinates, SPSkyObject, SPTarget}
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.TextBoxWidget

import scalaz._
import Scalaz._

sealed class CoordinateEditor[T <: SPSkyObject, S](initializer: () => T,
                                                   coordsLens: S @?> Coordinates,
                                                   mutableSetter: T => S => Unit,
                                                   getter: T => S
                                            ) extends TelescopePosEditor[T] with ReentrancyHack {

  private[this] var spo: T = initializer()

  val ra, dec = new TextBoxWidget <| {w =>
    w.setColumns(10)
    w.setMinimumSize(w.getPreferredSize)
  }
  private val raLens = coordsLens >=> Coordinates.ra.partial
  private val decLens = coordsLens >=> Coordinates.dec.partial

  ra.addWatcher(watcher { s =>
    nonreentrant {
      Angle.parseHMS(clean(s)).map(RightAscension.fromAngle) match {
        case -\/(_) => ra.setForeground(Color.RED)
        case \/-(a) =>
          ra.setForeground(Color.BLACK)
          raLens.set(getter(spo), a).foreach(mutableSetter(spo))
      }
    }
  })

  dec.addWatcher(watcher { s =>
    nonreentrant {
      Angle.parseDMS(clean(s)).map(Declination.fromAngle) match {
        case -\/(_) | \/-(None) => dec.setForeground(Color.RED)
        case \/-(Some(a)) =>
          dec.setForeground(Color.BLACK)
          decLens.set(getter(spo), a).foreach(mutableSetter(spo))
      }
    }
  })

  def edit(ctx: GOption[ObsContext], spo0: T, node: ISPNode): Unit = {
    spo = spo0
    nonreentrant {
      coordsLens.get(getter(spo)).foreach { cs =>
        ra.setText(cs.ra.toAngle.formatHMS)
        ra.setForeground(Color.BLACK)
        dec.setText(cs.dec.formatDMS)
        dec.setForeground(Color.BLACK)
      }
    }
  }

  def clean(angle: String): String =
    angle.trim.replace(",", ".")

}

final class SPTargetCoordinateEditor extends CoordinateEditor[SPTarget, Target](
  () => new SPTarget(), Target.coords, _.setTarget, _.getTarget
)

final class SPCoordinateEditor extends CoordinateEditor[SPCoordinates, Coordinates](
  () => new SPCoordinates(), PLens.plensId[Coordinates], _.setCoordinates, _.getCoordinates
)

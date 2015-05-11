package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ITarget.Tag

import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor

import javax.swing.JPanel

import scala.collection.JavaConverters._


object TargetDetailEditor {
  val JplMinorBody   = new JplMinorBodyDetailEditor
  val MpcMinorPlanet = new MpcMinorPlanetDetailEditor
  val Named          = new NamedDetailEditor
  val Sidereal       = new SiderealDetailEditor

  val All = List(JplMinorBody, MpcMinorPlanet, Named, Sidereal)

  val AllJava = All.asJava

  def forTag(t: Tag): TargetDetailEditor =
    t match {
      case Tag.JPL_MINOR_BODY   => JplMinorBody
      case Tag.MPC_MINOR_PLANET => MpcMinorPlanet
      case Tag.NAMED            => Named
      case Tag.SIDEREAL         => Sidereal
    }
}

abstract class TargetDetailEditor(val getTag: Tag) extends JPanel with TelescopePosEditor {
  def edit(ctx: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {
    require(ctx      != null, "obsContext should never be null")
    require(spTarget != null, "spTarget should never be null")
    val tag = spTarget.getTarget.getTag
    require(tag == getTag, "target tag should always be " + getTag + ", received " + tag)
  }
}

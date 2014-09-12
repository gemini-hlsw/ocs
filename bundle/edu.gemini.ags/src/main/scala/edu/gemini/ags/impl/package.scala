package edu.gemini.ags

import edu.gemini.shared.skyobject.{SkyObject, Magnitude}
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates
import edu.gemini.shared.util.immutable.PredicateOp
import edu.gemini.skycalc.{Angle, Coordinates}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.GuideProbeTargets
import edu.gemini.spModel.target.system.INonSiderealTarget

package object impl {

  def find(gpt: GuideProbeTargets, targetName: String): Option[SPTarget] =
    Option(targetName).map(_.trim).flatMap { tn =>
      gpt.getOptions.find(new PredicateOp[SPTarget] {
        def apply(spt: SPTarget): java.lang.Boolean =
          Option(spt.getName).map(_.trim).exists(_ == tn)
      }).asScalaOpt
    }

  def isSidereal(ctx: ObsContext): Boolean =
    !ctx.getTargets.getBase.getTarget.isInstanceOf[INonSiderealTarget]

  def isAo(ctx: ObsContext): Boolean = !ctx.getAOComponent.isEmpty

  implicit def asSkycalcCoords(hmsDegCoords: HmsDegCoordinates) = new Object {
    def asSkycalc: Coordinates = new Coordinates(hmsDegCoords.getRa, hmsDegCoords.getDec)
  }

  implicit object MagnitudeOrdering extends Ordering[Magnitude] {
    def compare(m1: Magnitude, m2: Magnitude): Int = m1.compareTo(m2)
  }

  def ctx180(c: ObsContext): ObsContext =
    c.withPositionAngle(c.getPositionAngle.add(180.0, Angle.Unit.DEGREES))

  def brightness(so: SkyObject, b: Magnitude.Band): Option[Double] =
    so.getMagnitude(b).asScalaOpt.map(_.getBrightness)

  def brightest[A](lst: List[A], band: Magnitude.Band)(toSkyObject: A => SkyObject): Option[A] = {
    lazy val max = new Magnitude(band, Double.MaxValue)
    if (lst.isEmpty) None
    else Some(lst.minBy(toSkyObject(_).getMagnitude(band).getOrElse(max)))
  }

  def skyObjectFromScienceTarget(target: SPTarget): SkyObject = {
    val name            = target.getName
    val coords          = target.getSkycalcCoordinates
    val skyObjectCoords = new HmsDegCoordinates.Builder(coords.getRa, coords.getDec).build
    val magnitudes      = target.getMagnitudes
    new SkyObject.Builder(name, skyObjectCoords).build.withMagnitudes(magnitudes)
  }
}

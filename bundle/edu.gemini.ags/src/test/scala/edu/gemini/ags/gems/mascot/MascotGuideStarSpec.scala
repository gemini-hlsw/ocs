package edu.gemini.ags.gems.mascot

import edu.gemini.catalog.api.{RadiusConstraint, CatalogQuery}
import edu.gemini.catalog.api.CatalogName.PPMXL
import edu.gemini.catalog.votable.{TestVoTableBackend, VoTableClient}
import edu.gemini.spModel.core._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.telescope.IssPort
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.shared.util.immutable.{None => JNone}
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz._
import Scalaz._

/**
 * Tests the MascotGuideStar class
 */
class MascotGuideStarSpec extends Specification {
  val asterism = List(
    List(0, 49.950975416666665, 41.511688333333325, 0.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, 0.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, 0.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, 0.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, 0.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, 0.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, 0.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, 0.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, 0.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, 0.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, 0.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, 0.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, 0.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, 0.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, 0.0, 8.944271909994837),
    List(0, 49.950975416666665, 41.511688333333325, -36.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, -36.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, -36.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, -36.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, -36.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, -36.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, -36.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, -36.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, -36.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, -36.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, -36.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, -36.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, -36.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, -36.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, -36.0, 8.944271909994837),
    List(0, 49.950975416666665, 41.511688333333325, 36.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, 36.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, 36.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, 36.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, 36.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, 36.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, 36.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, 36.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, 36.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, 36.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, 36.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, 36.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, 36.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, 36.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, 36.0, 8.944271909994837),
    List(0, 49.950975416666665, 41.511688333333325, -72.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, -72.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, -72.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, -72.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, -72.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, -72.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, -72.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, -72.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, -72.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, -72.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, -72.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, -72.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, -72.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, -72.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, -72.0, 8.944271909994837),
    List(0, 49.950975416666665, 41.511688333333325, 72.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, 72.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, 72.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, 72.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, 72.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, 72.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, 72.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, 72.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, 72.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, 72.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, 72.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, 72.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, 72.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, 72.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, 72.0, 8.944271909994837),
    List(0, 49.950975416666665, 41.511688333333325, -108.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, -108.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, -108.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, -108.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, -108.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, -108.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, -108.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, -108.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, -108.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, -108.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, -108.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, -108.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, -108.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, -108.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, -108.0, 8.944271909994837),
    List(0, 49.950975416666665, 41.511688333333325, 108.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, 108.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, 108.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, 108.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, 108.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, 108.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, 108.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, 108.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, 108.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, 108.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, 108.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, 108.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, 108.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, 108.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, 108.0, 8.944271909994837),
    List(0, 49.950975416666665, 41.511688333333325, -144.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, -144.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, -144.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, -144.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, -144.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, -144.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, -144.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, -144.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, -144.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, -144.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, -144.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, -144.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, -144.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, -144.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, -144.0, 8.944271909994837),
    List(0, 49.950975416666665, 41.511688333333325, 144.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, 144.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, 144.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, 144.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, 144.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, 144.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, 144.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, 144.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, 144.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, 144.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, 144.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, 144.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, 144.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, 144.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, 144.0, 8.944271909994837),
    List(0, 49.950975416666665, 41.511688333333325, -180.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, -180.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, -180.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, -180.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, -180.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, -180.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, -180.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, -180.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, -180.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, -180.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, -180.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, -180.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, -180.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, -180.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, -180.0, 8.944271909994837),
    List(0, 49.950975416666665, 41.511688333333325, 180.0, 0.0),
    List(0, 49.950975416666665, 41.51057722222222, 180.0, 3.9999999999878355),
    List(0, 49.950975416666665, 41.51279944444443, 180.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.511688333333325, 180.0, 3.9999999999878355),
    List(0, 49.95208652777777, 41.511688333333325, 180.0, 3.9999999999878355),
    List(0, 49.94986430555556, 41.51057722222222, 180.0, 5.6568542494751775),
    List(0, 49.95208652777777, 41.51279944444443, 180.0, 5.6568542494751775),
    List(0, 49.950975416666665, 41.5094661111111, 180.0, 8.00000000000125),
    List(0, 49.950975416666665, 41.51391055555555, 180.0, 8.00000000000125),
    List(0, 49.94875319444444, 41.511688333333325, 180.0, 8.00000000000125),
    List(0, 49.95319763888889, 41.511688333333325, 180.0, 8.00000000000125),
    List(0, 49.94986430555556, 41.5094661111111, 180.0, 8.944271909994837),
    List(0, 49.95208652777777, 41.51391055555555, 180.0, 8.944271909994837),
    List(0, 49.94875319444444, 41.51057722222222, 180.0, 8.944271909994837),
    List(0, 49.95319763888889, 41.51279944444443, 180.0, 8.944271909994837))

  // Targets lodaded with the query:
  // curl -v "http://cpocatalog2.cl.gemini.edu/cgi-bin/conesearch.py?CATALOG=ppmxl&RA=49.951&DEC=41.512&SR=0.020"
  val loadedTargets = List(
    SiderealTarget("-1815764439", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.935293)), Declination.fromAngle(Angle.fromDegrees(41.516092000000015)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(359.99999999999835)), DeclinationAngularVelocity(AngularVelocity(359.9999999999996)), Epoch(2000.0))), None, None, List(Magnitude(14.46, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(12.144, MagnitudeBand.H, Some(0.023), MagnitudeSystem.Vega), Magnitude(13.56, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(12.677, MagnitudeBand.J, Some(0.022), MagnitudeSystem.Vega), Magnitude(12.063, MagnitudeBand.K, Some(0.022), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("-1657323522", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.93620099999998)), Declination.fromAngle(Angle.fromDegrees(41.515949999999975)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(3.979039320256561E-12)), DeclinationAngularVelocity(AngularVelocity(359.999999999999)), Epoch(2000.0))), None, None, List(Magnitude(14.75, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.H, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.J, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.K, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(13.08, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("-747922171", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.935239000000024)), Declination.fromAngle(Angle.fromDegrees(41.501823)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(1.7053025658242404E-13)), DeclinationAngularVelocity(AngularVelocity(359.9999999999988)), Epoch(2000.0))), None, None, List(Magnitude(Double.NaN, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(16.199, MagnitudeBand.H, Some(0.244), MagnitudeSystem.Vega), Magnitude(18.25, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(16.87, MagnitudeBand.J, Some(0.188), MagnitudeSystem.Vega), Magnitude(15.374, MagnitudeBand.K, Some(0.182), MagnitudeSystem.Vega), Magnitude(19.22, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("-2083220734", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.939503)), Declination.fromAngle(Angle.fromDegrees(41.518890999999996)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(3.808509063674137E-12)), DeclinationAngularVelocity(AngularVelocity(1.7053025658242404E-12)), Epoch(2000.0))), None, None, List(Magnitude(19.31, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.H, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.J, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.K, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("733649392", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.94487099999998)), Declination.fromAngle(Angle.fromDegrees(41.529246)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(359.9999999999995)), DeclinationAngularVelocity(AngularVelocity(2.8421709430404007E-13)), Epoch(2000.0))), None, None, List(Magnitude(17.52, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(15.055, MagnitudeBand.H, Some(0.103), MagnitudeSystem.Vega), Magnitude(15.29, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(15.621, MagnitudeBand.J, Some(0.075), MagnitudeSystem.Vega), Magnitude(14.551, MagnitudeBand.K, Some(0.102), MagnitudeSystem.Vega), Magnitude(15.85, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("-774265756", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.94998499999997)), Declination.fromAngle(Angle.fromDegrees(41.51208200000002)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(359.999999999999)), DeclinationAngularVelocity(AngularVelocity(3.979039320256561E-13)), Epoch(2000.0))), None, None, List(Magnitude(Double.NaN, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.H, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.J, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.K, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("-705782052", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.950604)), Declination.fromAngle(Angle.fromDegrees(41.511713999999984)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(9.094947017729282E-13)), DeclinationAngularVelocity(AngularVelocity(359.9999999999976)), Epoch(2000.0))), None, None, List(Magnitude(Double.NaN, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(11.978, MagnitudeBand.H, Some(0.079), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(12.769, MagnitudeBand.J, Some(0.074), MagnitudeSystem.Vega), Magnitude(11.298, MagnitudeBand.K, Some(0.061), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("-282565385", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.97353900000002)), Declination.fromAngle(Angle.fromDegrees(41.51455199999998)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(1.7053025658242404E-13)), DeclinationAngularVelocity(AngularVelocity(359.99999999999994)), Epoch(2000.0))), None, None, List(Magnitude(16.02, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(14.212, MagnitudeBand.H, Some(0.047), MagnitudeSystem.Vega), Magnitude(15.16, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(14.666, MagnitudeBand.J, Some(0.032), MagnitudeSystem.Vega), Magnitude(14.21, MagnitudeBand.K, Some(0.065), MagnitudeSystem.Vega), Magnitude(15.1, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("2104949014", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.957037000000014)), Declination.fromAngle(Angle.fromDegrees(41.50245699999999)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(359.9999999999998)), DeclinationAngularVelocity(AngularVelocity(1.6484591469634324E-12)), Epoch(2000.0))), None, None, List(Magnitude(Double.NaN, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.H, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(18.08, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.J, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.K, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(18.52, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("-1405314469", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.96945499999998)), Declination.fromAngle(Angle.fromDegrees(41.507069)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(359.9999999999996)), DeclinationAngularVelocity(AngularVelocity(2.8421709430404007E-13)), Epoch(2000.0))), None, None, List(Magnitude(19.26, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.H, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(18.37, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.J, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.K, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(18.8, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("1099733138", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.97110600000002)), Declination.fromAngle(Angle.fromDegrees(41.50199199999997)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(359.99999999999994)), DeclinationAngularVelocity(AngularVelocity(359.9999999999998)), Epoch(2000.0))), None, None, List(Magnitude(18.91, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.H, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.J, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.K, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("1401586964", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.95545299999998)), Declination.fromAngle(Angle.fromDegrees(41.53067599999997)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(359.9999999999987)), DeclinationAngularVelocity(AngularVelocity(1.7053025658242404E-13)), Epoch(2000.0))), None, None, List(Magnitude(19.71, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(15.803, MagnitudeBand.H, Some(0.161), MagnitudeSystem.Vega), Magnitude(17.51, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(16.042, MagnitudeBand.J, Some(0.09), MagnitudeSystem.Vega), Magnitude(15.202, MagnitudeBand.K, Some(0.162), MagnitudeSystem.Vega), Magnitude(17.87, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("-1660076183", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.96053699999999)), Declination.fromAngle(Angle.fromDegrees(41.53021000000001)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(359.9999999999991)), DeclinationAngularVelocity(AngularVelocity(3.410605131648481E-13)), Epoch(2000.0))), None, None, List(Magnitude(19.38, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(15.847, MagnitudeBand.H, Some(0.167), MagnitudeSystem.Vega), Magnitude(17.63, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(16.237, MagnitudeBand.J, Some(0.105), MagnitudeSystem.Vega), Magnitude(15.368, MagnitudeBand.K, Some(0.18), MagnitudeSystem.Vega), Magnitude(17.95, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("-1816765778", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.96322500000002)), Declination.fromAngle(Angle.fromDegrees(41.52373399999999)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(359.9999999999999)), DeclinationAngularVelocity(AngularVelocity(359.9999999999999)), Epoch(2000.0))), None, None, List(Magnitude(14.36, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(11.088, MagnitudeBand.H, Some(0.021), MagnitudeSystem.Vega), Magnitude(11.95, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(11.678, MagnitudeBand.J, Some(0.023), MagnitudeSystem.Vega), Magnitude(10.979, MagnitudeBand.K, Some(0.018), MagnitudeSystem.Vega), Magnitude(12.56, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("142815509", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.964692000000014)), Declination.fromAngle(Angle.fromDegrees(41.525802999999996)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(359.99999999999864)), DeclinationAngularVelocity(AngularVelocity(359.99999999999835)), Epoch(2000.0))), None, None, List(Magnitude(Double.NaN, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(14.713, MagnitudeBand.H, Some(0.08), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(15.287, MagnitudeBand.J, Some(0.067), MagnitudeSystem.Vega), Magnitude(14.453, MagnitudeBand.K, Some(0.085), MagnitudeSystem.Vega), Magnitude(18.01, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None),
    SiderealTarget("-854279698", Coordinates(RightAscension.fromAngle(Angle.fromDegrees(49.965525000000014)), Declination.fromAngle(Angle.fromDegrees(41.52722299999999)).getOrElse(Declination.zero)), Some(ProperMotion(RightAscensionAngularVelocity(AngularVelocity(7.958078640513122E-13)), DeclinationAngularVelocity(AngularVelocity(6.991740519879386E-12)), Epoch(2000.0))), None, None, List(Magnitude(Double.NaN, MagnitudeBand.B, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.H, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(17.87, MagnitudeBand.I, None, MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.J, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(Double.NaN, MagnitudeBand.K, Some(Double.NaN), MagnitudeSystem.Vega), Magnitude(18.76, MagnitudeBand.R, None, MagnitudeSystem.Vega)), None, None))

  "Mascot" should {
    "find best asterism" in {
      val coordinates = Coordinates(RightAscension.fromAngle(Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)), Declination.fromAngle(Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      val base = new SPTarget(coordinates.ra.toAngle.toDegrees, coordinates.dec.toDegrees)
      val env = TargetEnvironment.create(base)
      val inst = new Gsaoi()
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val ctx = ObsContext.create(env, inst, JNone.instance(), SPSiteQuality.Conditions.BEST, null, null, JNone.instance())

      val result = MascotGuideStar.findBestAsterismInQueryResult(loadedTargets, ctx, MascotGuideStar.CWFS, 180.0, 10.0)

      val remoteAsterism = for {
        (strehlList, pa, ra, dec) <- result
        d = MascotGuideStar.dist(ra, dec, coordinates.ra.toAngle.toDegrees, coordinates.dec.toDegrees) * 3600.0
      } yield List(strehlList.size, ra, dec, pa, d)
      asterism should beEqualTo(remoteAsterism)
    }
    "find best asterism on r-like magnitudes" in {
      val coordinates = Coordinates(RightAscension.fromAngle(Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)), Declination.fromAngle(Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      val base = new SPTarget(coordinates.ra.toAngle.toDegrees, coordinates.dec.toDegrees)
      val env = TargetEnvironment.create(base)
      val inst = new Gsaoi()
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val ctx = ObsContext.create(env, inst, JNone.instance(), SPSiteQuality.Conditions.BEST, null, null, JNone.instance())

      val result = MascotGuideStar.findBestAsterismInQueryResult(MascotGuideStarSpec.replaceRBands(loadedTargets), ctx, MascotGuideStar.CWFS, 180.0, 10.0)

      val remoteAsterism = for {
        (strehlList, pa, ra, dec) <- result
        d = MascotGuideStar.dist(ra, dec, coordinates.ra.toAngle.toDegrees, coordinates.dec.toDegrees) * 3600.0
      } yield List(strehlList.size, ra, dec, pa, d)
      asterism should beEqualTo(remoteAsterism)
    }
    "find best asterism by query result" in {
      val coordinates = Coordinates(RightAscension.fromAngle(Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)), Declination.fromAngle(Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)).getOrElse(Declination.zero))

      val query = CatalogQuery(coordinates, RadiusConstraint.between(Angle.fromArcmin(MascotCat.defaultMinRadius), Angle.fromArcmin(MascotCat.defaultMaxRadius)), PPMXL)
      val r = VoTableClient.catalog(query, Some(TestVoTableBackend("/mascotquery.xml")))(implicitly).map { t =>
        val base = new SPTarget(coordinates.ra.toAngle.toDegrees, coordinates.dec.toDegrees)
        val env = TargetEnvironment.create(base)
        val inst = new Gsaoi()
        inst.setPosAngle(0.0)
        inst.setIssPort(IssPort.SIDE_LOOKING)
        val ctx = ObsContext.create(env, inst, JNone.instance(), SPSiteQuality.Conditions.BEST, null, null, JNone.instance())

        val targets = t.result.targets.rows
        val result = MascotGuideStar.findBestAsterismInQueryResult(targets, ctx, MascotGuideStar.CWFS, 180.0, 10.0)

        for {
          (strehlList, pa, ra, dec) <- result
          d = MascotGuideStar.dist(ra, dec, coordinates.ra.toAngle.toDegrees, coordinates.dec.toDegrees) * 3600.0
        } yield List(strehlList.size, ra, dec, pa, d)
      }

      import scala.concurrent.duration._
      import scala.concurrent._

      Await.result(r, new FiniteDuration(30, scala.concurrent.duration.SECONDS)) must beEqualTo(asterism)
    }
  }
}

object MascotGuideStarSpec {
  // Converts targets with R magnitude to r', R or UC discarding duplicates
  // It is important to discard duplicates or the same target could be assigned to different bands
  // breaking the tests
  private def assignRLikeMagnitudes(targets: List[SiderealTarget]):Map[String, SiderealTarget] =
  targets.distinct.zipWithIndex.collect {
    case (t, i) if i % 3 == 0 =>
      val mags = t.magnitudes.collect {
        case m if m.band === MagnitudeBand.R => m.copy(band = MagnitudeBand._r)
        case m                               => m
      }
      t.copy(magnitudes = mags)
    case (t, i) if i % 3 == 1 =>
      val mags = t.magnitudes.collect {
        case m if m.band === MagnitudeBand.R => m.copy(band = MagnitudeBand.UC)
        case m                               => m
      }
      t.copy(magnitudes = mags)
    case (t, _) => t
  }.map(t => t.name -> t).toMap

  private def translateTargets(targets: List[SiderealTarget], targetsMap: Map[String, SiderealTarget]) = targets.map { t =>
    targetsMap.getOrElse(t.name, t)
  }

  // Convert targets from having band R to r', R or UC
  def replaceRBands(targets: List[SiderealTarget]): List[SiderealTarget] = {
    translateTargets(targets, assignRLikeMagnitudes(targets))
  }
}
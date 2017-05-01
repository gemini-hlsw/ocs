package edu.gemini.spModel.io.impl.migration.to2017A

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.{Ephemeris, Declination, RightAscension, Coordinates, Site, AlmostEqual}
import AlmostEqual._
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.target.obsComp.TargetObsComp
import org.specs2.mutable.Specification
import edu.gemini.spModel.rich.pot.sp._

import scalaz._, Scalaz._

class EphemerisConversionTest extends Specification with MigrationTest{
  private def entry(time: Long, ra: Double, dec: Double): (Long, Coordinates) =
    time -> Coordinates(RightAscension.fromDegrees(ra), Declination.fromDegrees(dec).get)

  val expected = Ephemeris(Site.GN, ==>>.fromList(List(
    entry(1467331200000l,  78.66414833333334, 20.211091944444433),
    entry(1477921200000l, 124.40285291666669, 19.050720555555586),
    entry(1488313080000l, 112.37534291666668, 26.111878888888896)
  )))

  "2017A Ephemeris Migration" should {
    "Convert non-sidereal ephemeris data to compressed ephemeris data" in withTestProgram2("vesta.xml") { p =>
      (for {
        oc <- p.getObservations.get(0).findObsComponentByType(SPComponentType.TELESCOPE_TARGETENV)
        ns <- oc.getDataObject.asInstanceOf[TargetObsComp].getTargetEnvironment.getAsterism.getNonSiderealTarget
      } yield ns.ephemeris).get ~= expected
    }
  }

}

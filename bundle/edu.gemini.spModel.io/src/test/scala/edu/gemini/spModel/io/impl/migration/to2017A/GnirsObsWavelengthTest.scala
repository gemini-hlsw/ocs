package edu.gemini.spModel.io.impl.migration.to2017A

import edu.gemini.pot.sp.{ISPProgram, SPComponentType}
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import edu.gemini.spModel.io.impl.migration.MigrationTest
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import edu.gemini.spModel.rich.pot.sp._

class GnirsObsWavelengthTest extends Specification with MigrationTest {

  def check(p: ISPProgram, obsNum: Int, expected: Boolean): MatchResult[Any] =
    p.getObservations.get(obsNum).findObsComponentByType(SPComponentType.INSTRUMENT_GNIRS).map { oc =>
      oc.getDataObject.asInstanceOf[InstGNIRS].isOverrideAcqObsWavelength
    } must_== Some(expected)

  "2017A Gnirs Migration" should {
    "Set override acq observing wavelength to false for executed observations" in withTestProgram2("gnirs.xml") { p =>
      check(p, 0, expected = false)
    }

    "Leave unexecuted observations alone" in withTestProgram2("gnirs.xml") { p =>
      check(p, 1, expected = true)
    }
  }
}

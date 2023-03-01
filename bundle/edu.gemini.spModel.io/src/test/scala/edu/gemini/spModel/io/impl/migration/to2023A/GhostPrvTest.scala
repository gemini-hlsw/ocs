// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.io.impl.migration
package to2023A

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.spModel.target.env.Asterism
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.rich.pot.sp._
import org.specs2.mutable.Specification

class GhostPrvTest extends Specification with MigrationTest {

  implicit class MoreISPProgramOps(p: ISPProgram) {
    def findAsterism(obsName: String): Asterism =
      p.allObservations
        .find(_.title == obsName)
        .flatMap(_.findTargetObsComp)
        .map(_.getAsterism)
        .getOrElse(sys.error(s"Couldn't find asterism for observation named '$obsName'."))
  }

  "2023A GHOST Asterisms" should {

    "Add PRV_OFF to HR" in withTestProgram2("GS-2022B-Q-1.xml") {
      _.findAsterism("HR") must beLike {
        case GhostAsterism.HighResolutionTargetPlusSky(_, _, p, _) =>
          p mustEqual GhostAsterism.PrvMode.PrvOff
      }

    }

  }

}

package edu.gemini.spModel.io.impl.migration.to2018A

import edu.gemini.spModel.target.env.Asterism
import edu.gemini.spModel.core._
import edu.gemini.spModel.io.impl.migration.MigrationTest
import org.specs2.mutable.Specification
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.pot.sp.ISPProgram

import scalaz._
import Scalaz._

class TargetToAsterismTest extends Specification with MigrationTest {

  implicit class MoreISPProgramOps(p: ISPProgram) {
    def findAsterism(obsName: String): Asterism =
      p.allObservations
       .find(_.title == obsName)
       .flatMap(_.findTargetObsComp)
       .map(_.getAsterism)
       .getOrElse(sys.error(s"Couldn't find asterism for observation named '$obsName'."))
  }

  "2017B Asterism Migration" should {

    "Convert sidereal targets." in withTestProgram2("asterism.xml") {
      _.findAsterism("sidereal") must beLike {
        case Asterism.Single(t) => t.getTarget must beLike {
          case t: SiderealTarget => ok
        }
      }
    }

    "Convert non-sidereal targets." in withTestProgram2("asterism.xml") {
      _.findAsterism("non-sidereal") must beLike {
        case Asterism.Single(t) => t.getTarget must beLike {
          case t: NonSiderealTarget => ok
        }
      }
    }

    "Convert too targets." in withTestProgram2("asterism.xml") {
      _.findAsterism("too") must beLike {
        case Asterism.Single(t) => t.getTarget must beLike {
          case t: TooTarget => ok
        }
      }
    }

  }

}

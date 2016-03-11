package edu.gemini.spModel.io.impl.migration.to2016B

import edu.gemini.pot.sp.{SPComponentType, ISPProgram}
import edu.gemini.spModel.core.{TooTarget, Target}
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.obs.{SchedulingBlock, SPObservation}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.obsComp.TargetObsComp
import org.specs2.mutable.Specification
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.shared.util.immutable.ScalaConverters._

class TargetMigrationTest extends Specification with MigrationTest {

  implicit class MoreProgramOps(p: ISPProgram) {

    def findBaseByObsTitle(title: String): Option[Target] =
      for {
        isp <- p.allObservations.find(_.title == title)
        x   <- isp.findObsComponentByType(SPComponentType.TELESCOPE_TARGETENV)
        spt <- x.dataObject.map(_.asInstanceOf[TargetObsComp].getBase)
      } yield spt.getNewTarget

  }

  "2016B Target Migration" should {

    "Migrate Sidereal Targets" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("sidereal") must beSome
      failure("not implemented")
    }

    "Migrate Named Targets" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("named") must beSome
      failure("not implemented")
    }

    "Migrate JPL Minor Bodies" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("jpl-minor-body") must beSome
      failure("not implemented")
    }

    "Migrate MPC Minor Planets" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("mpc-minor-planet") must beSome
      failure("not implemented")
    }

    "Migrate TOO Targets" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("too") must_== Some(TooTarget("bob"))
    }

  }

}

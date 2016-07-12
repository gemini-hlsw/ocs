package edu.gemini.spModel.io.impl.migration.to2016B

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.obs.{SchedulingBlock, SPObservation}
import org.specs2.mutable.Specification
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.shared.util.immutable.ScalaConverters._

class SchedulingBlockMigrationTest extends Specification with MigrationTest {

  implicit class MoreProgramOps(p: ISPProgram) {

    def findObsByTitle(title: String): Option[SPObservation] =
      p.allObservations.flatMap(_.spObservation.toList).find(_.getTitle == title)

    def findSchedulingBlock(title: String): Option[SchedulingBlock] =
      p.findObsByTitle(title).flatMap(_.getSchedulingBlock.asScalaOpt)

  }

  "2016B Schedling Block Migration" should {

    "Preserve Existing Scheduling Blocks" in withTestProgram2("schedulingBlock.xml") { p =>
      p.findSchedulingBlock("some") must_== Some(SchedulingBlock(1457551614113L, SchedulingBlock.Duration.Explicit(92500L)))
    }

    "Use Valid-At For Non-Sidereal Base Positions" in withTestProgram2("schedulingBlock.xml") { p =>
      p.findSchedulingBlock("nonsidereal") must_== Some(SchedulingBlock(1454284800000L, SchedulingBlock.Duration.Unstated))
    }

    "Ignore Sidereal Observations" in withTestProgram2("schedulingBlock.xml") { p =>
      p.findSchedulingBlock("none") must_== None
    }

  }

}

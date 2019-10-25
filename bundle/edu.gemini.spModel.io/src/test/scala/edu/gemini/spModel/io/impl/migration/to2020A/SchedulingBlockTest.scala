package edu.gemini.spModel.io.impl.migration.to2020A

import edu.gemini.pot.sp.{ISPObservation, ISPProgram}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.{Semester, Site}
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.obs.{SchedulingBlock, SPObservation}
import edu.gemini.spModel.rich.pot.sp._
import org.specs2.mutable.Specification

import scala.math.Ordering.Implicits._


class SchedulingBlockTest extends Specification with MigrationTest {

  val ProgramName: String =
    "GS-2020A-Q-501.xml"

  val sem2020A: Semester =
    new Semester(2020, Semester.Half.A)

  def currentSemester: Semester =
    new Semester(Site.GS, System.currentTimeMillis)

  def block(sem: Semester): SchedulingBlock =
    SchedulingBlock(sem.getMidpointDate(Site.GS).getTime, SchedulingBlock.Duration.Unstated)

  val Mid2020A: SchedulingBlock =
    SchedulingBlock(1588354200000L, SchedulingBlock.Duration.Unstated)

  val April2020: SchedulingBlock =
    SchedulingBlock(1585762200000L, SchedulingBlock.Duration.Unstated)

  "2020A SchedulingBlock Migration" should {

    implicit class MoreISPProgramOps(p: ISPProgram) {
      def findObsNumber(num: Int): SPObservation =
        p.allObservations
         .find(_.getObservationNumber == num)
         .map(_.getDataObject.asInstanceOf[SPObservation])
         .getOrElse(sys.error(s"Couldn't find observation $num"))
    }

    "Add a scheduling block when not present" in withTestProgram2(ProgramName) { p =>
      val expected = Some(block(List(currentSemester, sem2020A).max))
      p.findObsNumber(1).getSchedulingBlock.asScalaOpt must_== expected
    }

    "Keep an existing scheduling block when present" in withTestProgram2(ProgramName) {
      _.findObsNumber(2).getSchedulingBlock.asScalaOpt must_== Some(April2020)
    }
  }

}

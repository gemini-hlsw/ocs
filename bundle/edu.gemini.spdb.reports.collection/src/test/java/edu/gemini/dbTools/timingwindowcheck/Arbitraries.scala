package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.spModel.core.{ ProgramType, SPProgramID }
import edu.gemini.spModel.gemini.obscomp.SPProgram.Active
import edu.gemini.spModel.obs.ObsPhase2Status
import edu.gemini.spModel.obsrecord.ObsExecStatus

import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

trait Arbitraries {

  import Setup._

  implicit val arbPhase2Status: Arbitrary[ObsPhase2Status] =
    Arbitrary(frequency(
      (5, oneOf(ObsPhase2Status.values)),
      (5, const(ObsPhase2Status.PHASE_2_COMPLETE))
    ))

  implicit val arbObsExecStatus: Arbitrary[ObsExecStatus] =
    Arbitrary(oneOf(ObsExecStatus.values))

  implicit val arbObs: Arbitrary[Obs] =
    Arbitrary {
      for {
        p <- arbitrary[ObsPhase2Status]
        e <- arbitrary[ObsExecStatus]
      } yield Obs(p, e)
    }

  implicit val arbPid: Arbitrary[SPProgramID] =
    Arbitrary {
      for {
        t <- oneOf(ProgramType.All)
        n <- posNum[Int]
      } yield SPProgramID.toProgramID(s"GS-2018B-${t.abbreviation}-$n")
    }

  implicit val arbActive: Arbitrary[Active] =
    Arbitrary(oneOf(Active.values))

  implicit val arbProg: Arbitrary[Prog] =
    Arbitrary {
      for {
        i <- arbitrary[SPProgramID]
        a <- arbitrary[Active]
        c <- arbitrary[Boolean]
        o <- arbitrary[List[Obs]]
      } yield Prog(i, a, c, o)
    }

}

package edu.gemini.dbTools.ephemeris

import edu.gemini.pot.sp.{ISPObsComponent, ISPObservation, ProgramTestSupport, SPComponentType, ISPFactory, ISPProgram, ProgramGen}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.{Ephemeris, SiderealTarget, NonSiderealTarget, HorizonsDesignation}
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.obs.{ObservationStatus, ObsPhase2Status, SPObservation}
import edu.gemini.spModel.obsrecord.ObsExecStatus
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.util.SPTreeUtil
import edu.gemini.util.security.principal.StaffPrincipal
import org.scalacheck.Gen
import org.scalacheck.Arbitrary._

import java.security.Principal

import scala.collection.JavaConverters._
import scala.util.Random

trait TestSupport extends ProgramTestSupport {
  val User = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  import ProgramGen._

  val nonSids = List(
    (HorizonsDesignation.Comet("C/1973 E1"),           "Kohoutek" ),
    (HorizonsDesignation.AsteroidNewStyle("1971 UC1"), "1896 Beer"),
    (HorizonsDesignation.AsteroidOldStyle(4),          "Vesta"),
    (HorizonsDesignation.MajorBody(606),               "Titan")
  )

  val genNonSiderealTarget: Gen[NonSiderealTarget] =
    Gen.oneOf(nonSids).map { case (hid, name) =>
      NonSiderealTarget(name, Ephemeris.empty, Some(hid), List.empty, None, None)
    }

  def findOrCreateTargetComp(f: ISPFactory, o: ISPObservation): ISPObsComponent =
    Option(SPTreeUtil.findTargetEnvNode(o)).getOrElse {
      val tc = f.createObsComponent(o.getProgram, SPComponentType.TELESCOPE_TARGETENV, null)
      o.addObsComponent(tc)
      tc
    }

  def editProgram(ef: SPProgram => Unit): ProgEdit = { (_: ISPFactory, p: ISPProgram) =>
    val dob = p.getDataObject.asInstanceOf[SPProgram]
    ef(dob)
    p.setDataObject(dob)
  }

  val setInactive: ProgEdit =
    editProgram(_.setActive(SPProgram.Active.NO))

  val setCompleted: ProgEdit =
    editProgram(_.setCompleted(true))

  val setLibrary: ProgEdit =
    editProgram(_.setLibrary(true))

  val genInactiveProgram: Gen[ProgEdit] =
    Gen.oneOf(setInactive, setCompleted, setLibrary)

  val genSiderealEdit: Gen[ProgEdit] =
    for {
      f <- maybePickObservation
    } yield { (_: ISPFactory, p: ISPProgram) =>
      f(p).foreach { obs =>
        val tc  = SPTreeUtil.findTargetEnvNode(obs)
        val toc = tc.getDataObject.asInstanceOf[TargetObsComp]
        // TODO:ASTERISM: handle multi-target asterisms
        toc.getAsterism.allSpTargets.head.setTarget(SiderealTarget.empty)
        tc.setDataObject(toc)
      }
    }

  val genInactiveObsStatus: Gen[ProgEdit] =
    for {
      f <- maybePickObservation
      s <- Gen.oneOf(ObservationStatus.OBSERVED, ObservationStatus.INACTIVE)
    } yield { (_: ISPFactory, p: ISPProgram) =>
      f(p).foreach { obs =>
        val dob = obs.getDataObject.asInstanceOf[SPObservation]

        s match {
          case ObservationStatus.OBSERVED =>
            dob.setPhase2Status(ObsPhase2Status.PHASE_2_COMPLETE)
            dob.setExecStatusOverride(Option(ObsExecStatus.OBSERVED).asGeminiOpt)
          case _                          =>
            dob.setPhase2Status(s.phase2())
            dob.setExecStatusOverride(Option(ObsExecStatus.PENDING).asGeminiOpt)
        }

        obs.setDataObject(dob)
      }
    }

  val genTargetEnv: Gen[TargetEnvironment] = {
    val r = new Random
    arbitrary[TargetEnvironment].map { te =>
      te.getTargets.asScala.foreach { t =>
        // set half the targets on average to non-sidereal
        if (r.nextInt(2) == 0) {
          t.setTarget(genNonSiderealTarget.sample.get)
        }
      }
      te
    }
  }

  val genTestProg: Gen[ISPFactory => ISPProgram] =
    genProg.map { pCons => (fact: ISPFactory) => {
        val p = pCons(fact)
        p.getAllObservations.asScala.foreach { obs =>
          val tc  = findOrCreateTargetComp(fact, obs)
          val toc = tc.getDataObject.asInstanceOf[TargetObsComp]
          toc.setTargetEnvironment(genTargetEnv.sample.get)
          tc.setDataObject(toc)

          val dob = obs.getDataObject.asInstanceOf[SPObservation]
          dob.setPhase2Status(ObsPhase2Status.PHASE_2_COMPLETE)
          dob.setExecStatusOverride(Gen.oneOf(ObsExecStatus.PENDING, ObsExecStatus.ONGOING).sample.asGeminiOpt)
          obs.setDataObject(dob)
        }
        p
      }
    }
}

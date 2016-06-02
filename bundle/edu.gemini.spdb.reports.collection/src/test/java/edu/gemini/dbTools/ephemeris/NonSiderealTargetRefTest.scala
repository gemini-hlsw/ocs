package edu.gemini.dbTools.ephemeris

import edu.gemini.pot.sp.{ISPObservation, ISPProgram}
import edu.gemini.spModel.config.IConfigBuilder
import edu.gemini.spModel.core.{SiderealTarget, Target, NonSiderealTarget}
import edu.gemini.spModel.obs.{ObsPhase2Status, SPObservation, ObservationStatus}
import edu.gemini.spModel.obs.ObservationStatus.{INACTIVE, OBSERVED}
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.util.SPTreeUtil

import scala.collection.JavaConverters._

object NonSiderealTargetRefTest extends TestSupport {

  def allTargets(p: ISPProgram): List[Target] =
    p.getAllObservations.asScala.toList.flatMap { o =>
      val tc  = SPTreeUtil.findTargetEnvNode(o)
      val toc = tc.getDataObject.asInstanceOf[TargetObsComp]
      toc.getTargetEnvironment.getTargets.asScala.toList.map { _.getTarget }
    }

  def nonSiderealCount(p: ISPProgram): Int =
    allTargets(p).count {
      case n: NonSiderealTarget => true
      case _                    => false
    }

  def siderealCount(p: ISPProgram): Int =
    allTargets(p).count {
      case s: SiderealTarget => true
      case _                 => false
    }

  "NonSiderealObservation.findScheduleable" should {
    "include every ready nonsidereal target in an active program" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>
        val targetList = NonSiderealTargetRef.findRelevantIn(prog)
        targetList.size == nonSiderealCount(prog)
      }
    }

    "include no targets in an inactive program" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>
        genInactiveProgram.sample.get.apply(odb.getFactory, prog)
        NonSiderealTargetRef.findRelevantIn(prog).isEmpty
      }
    }

    "find matching observation information" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>
        val expected = NonSiderealTargetRef.findRelevantIn(prog).toSet
        val actual   = (Set.empty[NonSiderealTargetRef]/:prog.getAllObservations.asScala) { (s, o) =>
          val oid = o.getObservationID
          val tc  = SPTreeUtil.findTargetEnvNode(o)
          val toc = tc.getDataObject.asInstanceOf[TargetObsComp]
          (s/:toc.getTargetEnvironment.getTargets.asScala.map(_.getTarget)) { (s1, t) =>
            t match {
              case NonSiderealTarget(n, _, Some(hid), _, _, _) =>
                s1 + NonSiderealTargetRef(oid, hid, n)
              case _                                           =>
                s1
            }
          }
        }
        expected == actual
      }
    }

    "skip sidereal targets" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>
        val nsSize  = NonSiderealTargetRef.findRelevantIn(prog).size
        val allSize = allTargets(prog).size
        val sidSize = siderealCount(prog)
        (nsSize + sidSize) == allSize
      }
    }

    "skip inactive observations" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>
        def inActive: Option[ISPObservation] =
          prog.getAllObservations.asScala.find { obs =>
            ObservationStatus.computeFor(obs) match {
              case INACTIVE | OBSERVED => true
              case _                   => false
            }
          }

        def targetEnv(o: ISPObservation): Option[TargetEnvironment] =
          Option(SPTreeUtil.findTargetEnvNode(o)).map(_.getDataObject.asInstanceOf[TargetObsComp].getTargetEnvironment)

        def nonSidCount(te: TargetEnvironment): Int =
          te.getTargets.asScala.map(_.getTarget).count {
            case n: NonSiderealTarget => true
            case _                    => false
          }

        genInactiveObsStatus.sample.get.apply(odb.getFactory, prog)

        val inactiveCount = (for {
          io <- inActive
          te <- targetEnv(io)
        } yield nonSidCount(te)).getOrElse(0)

        val nsSize  = NonSiderealTargetRef.findRelevantIn(prog).size
        val allSize = nonSiderealCount(prog)

        nsSize + inactiveCount == allSize
      }
    }

    /* TODO: This works but fills the log with frightening stack traces because
       TODO: we set it up to throw an exception.  Need a way to limit output.
    "include observations for which we cannot compute the obs status" ! forAllPrograms { (odb, progs) =>
      progs.forall { prog =>

        // Figure out all the references w/o exceptions.
        val refs0 = NonSiderealTargetRef.findRelevantIn(prog)

        // Doctor all the observations to blow up when you compute the obs
        // status.
        prog.getAllObservations.asScala.foreach { obs =>
          val sp = obs.getDataObject.asInstanceOf[SPObservation]
          sp.setPhase2Status(ObsPhase2Status.PHASE_2_COMPLETE)
          sp.setExecStatusOverride(edu.gemini.shared.util.immutable.ImOption.empty())
          obs.setDataObject(sp)
          obs.removeClientData(IConfigBuilder.USER_OBJ_KEY)
        }

        NonSiderealTargetRef.Log.setLevel(Level.WARNING)

        // They should be the same
        refs0 == refs1
      }
    }
    */
  }
}

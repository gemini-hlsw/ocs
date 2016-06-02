package edu.gemini.dbTools.ephemeris

import edu.gemini.pot.sp.{ISPObsComponent, ISPObservation, ISPProgram, SPObservationID}
import edu.gemini.spModel.core.{NonSiderealTarget, HorizonsDesignation}
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.util.SPTreeUtil

import java.util.logging.{Level, Logger}

import scalaz._

/** A reference to an individual non-sidereal target reference. */
case class NonSiderealTargetRef(oid: SPObservationID, hid: HorizonsDesignation, targetName: String)

import scala.collection.JavaConverters._

object NonSiderealTargetRef {
  private val Log = Logger.getLogger(NonSiderealTargetRef.getClass.getName)

  /** Finds all scheduleable non-sidereal observations in the given program,
    * if any.  A scheduleable observation is one that could be included in a
    * queue for immediate observation.  The program itself must be active and
    * the observation status must be `READY` or `ONGOING`.
    */
  def findRelevantIn(p: ISPProgram): List[NonSiderealTargetRef] =
    if (includeProgram(p)) activeObservations(p).flatMap(targetRef)
    else Nil

  private def includeProgram(p: ISPProgram): Boolean =
    Option(p.getProgramID).isDefined && (p.getDataObject match {
      case dob: SPProgram => dob.isActive && !dob.isCompleted && !dob.isLibrary
      case _              => false
    })

  private def activeObservations(p: ISPProgram): List[ISPObservation] = {
    import ObservationStatus._
    p.getAllObservations.asScala.filter { obs =>
      \/.fromTryCatchNonFatal(computeFor(obs)) match {
        case \/-(OBSERVED) | \/-(INACTIVE) =>
          false

        case -\/(t)                        =>
          // Computing the observation status is error-prone with smart-gcal
          // sequences. We'll conservatively assume that an observation is still
          // of use if we can't figure out the observation status.
          Log.log(Level.WARNING, s"Couldn't compute obs status for obs ${Option(obs.getObservationID).getOrElse(obs.getNodeKey)}", t)
          true

        case _                             =>
          true
      }
    }.toList
  }

  private def targetRef(o: ISPObservation): List[NonSiderealTargetRef] = {
    def env(tc: ISPObsComponent): Option[TargetEnvironment] =
      Option(tc.getDataObject).collect {
        case toc: TargetObsComp => toc.getTargetEnvironment
      }

    def ref(env: TargetEnvironment): List[NonSiderealTargetRef] =
      env.getTargets.asScala.map(_.getTarget).collect {
        case NonSiderealTarget(name, _, Some(hid), _, _, _) =>
          NonSiderealTargetRef(o.getObservationID, hid, name)
      }.toList

    for {
      t <- Option(SPTreeUtil.findTargetEnvNode(o)).toList
      e <- env(t).toList
      r <- ref(e)
    } yield r
  }
}

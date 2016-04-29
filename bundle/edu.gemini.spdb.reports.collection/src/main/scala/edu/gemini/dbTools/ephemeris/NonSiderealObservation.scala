package edu.gemini.dbTools.ephemeris

import edu.gemini.pot.sp.{ISPObsComponent, ISPObservation, ISPProgram, SPObservationID}
import edu.gemini.spModel.core.{NonSiderealTarget, HorizonsDesignation}
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.util.SPTreeUtil

/** A reference to an individual non-sidereal observation. */
case class NonSiderealObservation(oid: SPObservationID, hid: HorizonsDesignation, targetName: String)

import scala.collection.JavaConverters._

object NonSiderealObservation {

  /** Finds all scheduleable non-sidereal observations in the given program,
    * if any.  A scheduleable observation is one that could be included in a
    * queue for immediate observation.  The program itself must be active and
    * the observation status must be `READY` or `ONGOING`.
    */
  def findRelevantIn(p: ISPProgram): List[NonSiderealObservation] =
    if (includeProgram(p)) activeObservations(p).flatMap(obsRef)
    else Nil

  private def includeProgram(p: ISPProgram): Boolean =
    Option(p.getProgramID).isDefined && (p.getDataObject match {
      case dob: SPProgram => dob.isActive && !dob.isCompleted && !dob.isLibrary
      case _              => false
    })

  private def activeObservations(p: ISPProgram): List[ISPObservation] = {
    import ObservationStatus._
    p.getAllObservations.asScala.filter { obs =>
      computeFor(obs) match {
        case OBSERVED | INACTIVE => false
        case _                   => true
      }
    }.toList
  }

  private def obsRef(o: ISPObservation): Option[NonSiderealObservation] = {
    def env(tc: ISPObsComponent): Option[TargetEnvironment] =
      Option(tc.getDataObject).collect {
        case toc: TargetObsComp => toc.getTargetEnvironment
      }

    def ref(env: TargetEnvironment): Option[NonSiderealObservation] =
      env.getBase.getTarget match {
        case NonSiderealTarget(name, _, Some(hid), _, _, _) =>
          Some(NonSiderealObservation(o.getObservationID, hid, name))

        case _                                              =>
          None
      }

    for {
      t <- Option(SPTreeUtil.findTargetEnvNode(o))
      e <- env(t)
      r <- ref(e)
    } yield r
  }
}

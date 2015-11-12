package edu.gemini.dataman.app

import edu.gemini.dataman.core._
import edu.gemini.pot.sp.{ISPObsQaLog, ISPObsExecLog, ISPNode, SPNodeKeyLocks, SPNodeKey, ISPObservation, SPObservationID, ISPProgram}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.obslog.{ObsQaLog, ObsExecLog, ObsLog}

import scalaz._
import Scalaz._

/** Low-level actions for working with science programs.  These are meant to be
  * included in building up larger actions.
  */
object ModelActions {
  def lookupProgram(pid: SPProgramID, odb: IDBDatabaseService): DmanAction[ISPProgram] =
    safeGet(odb.lookupProgramByID(pid), s"Missing program '$pid'")

  def lookupObsOption(oid: SPObservationID, odb: IDBDatabaseService): DmanAction[Option[ISPObservation]] =
    DmanAction(Option(odb.lookupObservationByID(oid)))

  def lookupObs(oid: SPObservationID, odb: IDBDatabaseService): DmanAction[ISPObservation] =
    safeGet(odb.lookupObservationByID(oid), s"Missing observation '$oid'")

  private def locked[A](k: SPNodeKey, lock: SPNodeKey => Unit, unlock: SPNodeKey => Unit)(body: => DmanAction[A]): DmanAction[A] =
    DmanAction(lock(k)) >> { try { body } finally { unlock(k) } }

  def readLocked[A](k: SPNodeKey)(body: => DmanAction[A]): DmanAction[A] =
    locked(k, SPNodeKeyLocks.instance.readLock, SPNodeKeyLocks.instance.readUnlock)(body)

  def writeLocked[A](k: SPNodeKey)(body: => DmanAction[A]): DmanAction[A] =
    locked(k, SPNodeKeyLocks.instance.writeLock, SPNodeKeyLocks.instance.writeUnlock)(body)

  def safeGet[A](a: => A, failureMessage: => String): DmanAction[A] =
    tryOp(a).flatMap(Option(_).toTryDman(failureMessage)).liftDman

  def safePid(p: ISPProgram): DmanAction[SPProgramID] =
    safeGet(p.getProgramID, "Program missing id.")

  def safeOid(o: ISPObservation): DmanAction[SPObservationID] =
    safeGet(o.getObservationID, "Observation missing id.")

  def safeObslog(o: ISPObservation): DmanAction[ObsLog] =
    safeGet(ObsLog.getIfExists(o), "Observation missing obslog.")

  private def extract[N <: ISPNode, D: Manifest](name: String, o: ISPObservation)(get: ISPObservation => N): DmanAction[(N, D)] =
    for {
      oid <- safeOid(o)
      n   <- safeGet(get(o), s"Observation '$oid' $name missing")
      dob <- safeGet(n.getDataObject, s"Observation '$oid' $name missing data object")
      d   <- dob match {
        case d: D => d.right[DmanFailure].liftDman
        case _    => DmanAction.fail(DmanFailure.Unexpected(s"Observation '$oid' $name has empty or mistyped data object: " + ~Option(dob).map(_.getClass.getName)): DmanFailure)
      }
    } yield (n, d)

  def execLog(o: ISPObservation): DmanAction[(ISPObsExecLog, ObsExecLog)] =
    extract[ISPObsExecLog, ObsExecLog](s"exec log", o)(_.getObsExecLog)

  def qaLog(o: ISPObservation): DmanAction[(ISPObsQaLog, ObsQaLog)] =
    extract[ISPObsQaLog, ObsQaLog](s"qa log", o)(_.getObsQaLog)
}

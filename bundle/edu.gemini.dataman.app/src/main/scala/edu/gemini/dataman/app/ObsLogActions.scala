package edu.gemini.dataman.app

import edu.gemini.dataman.app.ModelActions._
import edu.gemini.dataman.core._
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config.{DatasetConfigService, ConfigBridge}
import edu.gemini.spModel.dataset.{DatasetQaState, Dataset, DatasetQaRecord, DatasetLabel, SummitState, DatasetExecRecord, DatasetGsaState}
import edu.gemini.spModel.dataset.Implicits._
import edu.gemini.spModel.dataset.SummitState.Idle
import edu.gemini.spModel.obsrecord.ObsExecRecord

import java.util.UUID
import java.util.logging.Logger

import scala.collection.JavaConverters._
import scala.Function.const
import scalaz._
import Scalaz._

import ObsLogActions._

/** A factory for data manager actions. When executed, the actions update the
  * corresponding observation(s) Observing Log(s) as appropriate.  Each action
  * will return the updated datasets, if any, upon completion.
  *
  * @param odb observing database in which to find the observations
  */
final class ObsLogActions(odb: IDBDatabaseService) {
  /** Returns an action that will record the user's desire to edit the QA state
    * of the given datasets.
    */
  def setQa(requests: List[QaRequest]): DmanAction[DatasetUpdates] =
    sequence {
      grouped(requests)(_.label).map { case (oid, ups) =>
        summitStateAction(oid, ups.map { r => Transition(r.label, _.transition.userRequest(r.qa)) })
      }
    }

  /** Returns an action that records summit FITS server poll results, updating
    * and/or creating datasets as necessary.
    */
  def updateSummit(files: List[GsaRecord]): DmanAction[DatasetUpdates] = {
    val validFiles = files.collect {
      case r@GsaRecord(Some(lab), _, _) => lab -> r
    }
    val fMap = validFiles.toMap

    // A constructor that creates Dataset instances when the poll results refer
    // to datasets that were not added to the obs log via the normal mechanism
    // (i.e., via seq exec events in the WDBA).
    val cons: DatasetLabel => Option[Dataset] = (lab) => {
      val dsetOpt = fMap.get(lab).map { f =>
        // Strip the trailing ".fits" suffix, if any, to be comptabile with how
        // the WDBA adds new dataset filenames..
        val filename = f.filename match {
          case FitsFile(n) => n
          case n           => n
        }
        new Dataset(lab, filename, System.currentTimeMillis)
      }

      dsetOpt.foreach { ds =>
        Log.warning(s"Creating missing dataset record for ${ds.getLabel}, ${ds.getDhsFilename}")
      }

      dsetOpt
    }

    sequence {
      grouped(validFiles)(_._1).map { case (oid, ups) =>
        summitStateAction(oid, ups.map { case (lab, f) => Transition(lab, _.transition.gsaState(some(f.state))) }, cons)
      }
    }
  }

  /** Returns an action that records a missing dataset on the summit.
    */
  def removeSummit(labels: List[DatasetLabel]): DmanAction[DatasetUpdates] =
    sequence {
      grouped(labels)(identity).map { case (oid, ups) =>
        summitStateAction(oid, ups.map { Transition(_, _.transition.gsaState(none)) })
      }
    }

  def updateSummit(updated: List[GsaRecord], missing: List[DatasetLabel]): DmanAction[DatasetUpdates] =
    for {
      up0 <- updateSummit(updated)
      up1 <- removeSummit(missing)
    } yield up0 |+| up1

  /** Returns an action that marks dataset state as having been requested in
    * the summit FITS server.
    */
  def sendQaRequest(labs: List[(DatasetLabel, UUID)]): DmanAction[DatasetUpdates] =
    sequence {
      grouped(labs)(_._1).map { case (oid, ups) =>
        summitStateAction(oid, ups.map { case (lab, id0) => Transition(lab, _.transition.pendingToProcessing(id0)) })
      }
    }

  /** Returns an action that records QA request results.
    */
  def recordQaResult(results: List[(DatasetLabel, UUID, String \/ Unit)]): DmanAction[DatasetUpdates] =
    sequence {
      grouped(results)(_._1).map { case (oid, ups) =>
        summitStateAction(oid, ups.map { case (lab, id0, r) =>
          Transition(lab, ss => r.fold(ss.transition.processingToFailed(id0, _), _ => ss.transition.processingToAccepted(id0)))
        })
      }
    }

  /** Returns an action that resets failed QA update requests.
    */
  def resetFailed(labs: List[(DatasetLabel, UUID)]): DmanAction[DatasetUpdates] =
    sequence {
      grouped(labs)(_._1).map { case (oid, ups) =>
          summitStateAction(oid, ups.map { case (lab, id0) => Transition(lab, _.transition.activeToPending(id0)) })
      }
    }

  /** Resets any outstanding ongoing active requests to failed.  This is used
    * at startup to retry requests that weren't completed.
    */
  def failActiveRequest(labs: List[(DatasetLabel, UUID)], msg: String): DmanAction[DatasetUpdates] =
    sequence {
      grouped(labs)(_._1).map { case (oid, ups) =>
          summitStateAction(oid, ups.map { case (lab, id0) => Transition(lab, _.transition.activeToFailed(id0, msg)) })
      }
    }

  def failIdleRequest(labs: List[(DatasetLabel, DatasetQaState)], msg: String): DmanAction[DatasetUpdates] =
    sequence {
      grouped(labs)(_._1).map { case (oid, ups) =>
          summitStateAction(oid, ups.map { case (lab, qa) => Transition(lab, _.transition.pendingSyncToFailed(qa, msg)) })
      }
    }

  // Builds a DmanAction that will apply the given state machine transitions
  // when executed.
  private def summitStateAction(oid: SPObservationID, transitions: List[Transition]): DmanAction[DatasetUpdates] =
    summitStateAction(oid, transitions, const(none))

  private def summitStateAction(oid: SPObservationID, transitions: List[Transition], cons: DatasetLabel => Option[Dataset]): DmanAction[DatasetUpdates] = {
    val transMap = transitions.map(t => t.label -> t).toMap

    // Gets all already exisitng DatasetExecRecords for the observation.
    def existingRecords(r: ObsExecRecord): List[DatasetExecRecord] =
      r.getAllDatasetExecRecords.asScala.toList

    // Creates any DatasetExecRecords for which we received poll information
    // from the archive but which don't exist in the database. They are made in
    // an initial "Missing" state which is then updated by the state machine
    // transition.
    def missingRecords(existing: List[DatasetExecRecord]): List[DatasetExecRecord] =
      (transMap.keySet/:existing) { (ks, er) => ks - er.label }.toList.flatMap { lab =>
        cons(lab).map(DatasetExecRecord.apply)
      }

    // Returns any updated DatasetExecRecords according to the provided state
    // machine transition functions in the provided `transitions`.
    def runTransitions(recs: List[DatasetExecRecord]): List[DatasetExecRecord] =
      recs.flatMap { oldRec =>
        transMap.get(oldRec.label).flatMap { trans =>
          val oldSs = oldRec.summit
          val newSs = trans.fun(oldSs)
          (oldSs =/= newSs) option oldRec.copy(summit = newSs)
        }
      }

    lookupObsOption(oid, odb) >>= {
      _.fold(DmanAction(EmptyUpdates)) { o =>
        writeLocked(o.getProgramKey) {
          for {
            exTup <- execLog(o)  // (exNode, exLog) <- execLog(o) doesn't work?
            qaTup <- qaLog(o)

            // Get the updates to the exec and qa datasets.  This part is pure,
            // no side-effects.
            (exNode, exLog) = exTup
            (qaNode, qaLog) = qaTup
            exRecord        = exLog.getRecord
            qaRecord        = qaLog.getRecord

            existDers       = existingRecords(exRecord)
            existExUpdates  = runTransitions(existDers)
            missExUpdates   = runTransitions(missingRecords(existDers))
            allExUpdates    = missExUpdates ++ existExUpdates

            qaUpdates = allExUpdates.collect {
              case DatasetExecRecord(ds, Idle(DatasetGsaState(qa, _, _)), _)
                if qaRecord.qaState(ds.getLabel) =/= qa =>
                // QA State is set to a distinct value and not expected to
                // change remotely so assign it to the QA state record.
                qaRecord(ds.getLabel).withQaState(qa)
            }

            _ <- DmanAction {
              // Apply updates, mutating as we go.

              // First, existing dataset exec record updates.
              existExUpdates.foreach(up => exRecord.putDatasetExecRecord(up, null))

              // Second, missing dataset exec records, which require a that we
              // compute a configuration that corresponds to the dataset.
              lazy val seq = ConfigBridge.extractSequence(o, null, ConfigValMapInstances.TO_DISPLAY_VALUE) // urp
              missExUpdates.foreach { up =>
                val config = DatasetConfigService.configForStep(seq, up.label.getIndex - 1)
                exRecord.putDatasetExecRecord(up, config.getOrNull)
              }

              // Finally QA record updates.
              qaUpdates.foreach(qaLog.set)

              if (allExUpdates.nonEmpty) exNode.setDataObject(exLog)
              if (qaUpdates.nonEmpty) qaNode.setDataObject(qaLog)
            }
          } yield (qaUpdates, allExUpdates)
        }
      }
    }
  }

  /** Returns an action that records archive state poll results.
    */
  def updateArchive(files: List[GsaRecord]): DmanAction[DatasetUpdates] =
    archiveStateAction(files.collect {
      case GsaRecord(Some(lab), _, state) => (lab, some(state))
    })

  /** Returns an action that records missing dataset label results.
    */
  def removeArchive(labels: List[DatasetLabel]): DmanAction[DatasetUpdates] =
    archiveStateAction(labels.map(label => (label, Option.empty[DatasetGsaState])))

  def updateArchive(updated: List[GsaRecord], missing: List[DatasetLabel]): DmanAction[DatasetUpdates] =
    for {
      up0 <- updateArchive(updated)
      up1 <- removeArchive(missing)
    } yield up0 |+| up1

  private def archiveStateAction(files: List[(DatasetLabel, Option[DatasetGsaState])]): DmanAction[DatasetUpdates] = {
    val fMap = files.toMap

    sequence {
      grouped(files)(_._1).map { case (oid, ups) =>
        lookupObsOption(oid, odb) >>= {
          _.fold(DmanAction(EmptyUpdates)) { o =>
            writeLocked(o.getProgramKey) {
              for {
                exTup <- execLog(o) // (exNode, exLog) <- execLog(o) doesn't work?

                (exNode, exLog) = exTup
                exRecord  = exLog.getRecord
                exUpdates = exRecord.getAllDatasetExecRecords.asScala.toList.flatMap { oldRec =>
                  fMap.get(oldRec.label).flatMap { newState =>
                    val oldState = oldRec.archive
                    (oldState =/= newState) option oldRec.copy(archive = newState)
                  }
                }

                _ <- DmanAction {
                  exUpdates.foreach(up => exRecord.putDatasetExecRecord(up, null))
                  if (exUpdates.nonEmpty) exNode.setDataObject(exLog)
                }
              } yield (List.empty[DatasetQaRecord], exUpdates)
            }
          }
        }
      }
    }
  }
}

object ObsLogActions {
  val Log = Logger.getLogger(ObsLogActions.getClass.getName)

  val FitsFile = """(\V*?)(?:\.fits)?$""".r

  val EmptyUpdates = (List.empty[DatasetQaRecord], List.empty[DatasetExecRecord])

  final case class Transition(label: DatasetLabel, fun: SummitState => SummitState)

  // Group the items in the given list by observation id, given a function to
  // extract a data label from an item (the observation id can be obtained from
  // the data label).
  private def grouped[A](lst: List[A])(f: A => DatasetLabel): List[(SPObservationID, List[A])] =
    lst.groupBy(a => f(a).getObservationId).toList

  private def sequence(lst: List[DmanAction[DatasetUpdates]]): DmanAction[DatasetUpdates] =
    lst.sequenceU.map { _.concatenate }
}

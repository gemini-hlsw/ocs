package edu.gemini.dataman.app

import edu.gemini.dataman.app.ModelActions._
import edu.gemini.dataman.core.GsaHost.{Archive, Summit}
import edu.gemini.dataman.core._
import edu.gemini.dataman.query.{GsaRecordQuery, GsaResponse}
import edu.gemini.pot.sp.{SPObservationID, ISPObservation, ISPProgram}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.{Site, SPProgramID}
import edu.gemini.spModel.dataset.DatasetLabel
import edu.gemini.spModel.obslog.ObsExecLog

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._


/** A factory for GSA server poll actions.
  */
sealed trait GsaPollActions {

  /** Returns an action which when executed polls the GSA for the current status
    * of all datasets related to the given program and records them in the
    * corresponding dataset records.
    *
    * @param pid id of the program whose dataset status in the GSA is sought
    */
  def program(pid: SPProgramID): DmanAction[DatasetUpdates]

  /** Returns an action which when executed polls the GSA for the current status
    * of all datasets related to the given observation and records them in the
    * corresponding dataset records.
    *
    * @param oid id of the observation whose dataset status in the GSA is sought
    */
  def observation(oid: SPObservationID): DmanAction[DatasetUpdates]

  /** Returns an action which when executed polls the GSA for the current status
    * of the dataset related to the given label and records it in the
    * corresponding dataset record.
    *
    * @param lab label whose dataset status in the GSA is sought
    */
  def dataset(lab: DatasetLabel): DmanAction[DatasetUpdates]

  /** Returns an action which when executed polls the GSA for the current status
    * of all datasets obtained in the past week, roughly, and records them in
    * the corresponding dataset records.
    */
  def thisWeek: DmanAction[DatasetUpdates]

  /** Returns an action which when executed polls the GSA for the current status
    * of all datasets taken in the current observing night and records them in
    * the corresponding dataset records.
    */
  def tonight: DmanAction[DatasetUpdates]
}

object GsaPollActions {

  def apply(host: GsaHost, site: Site, odb: IDBDatabaseService): GsaPollActions = new GsaPollActions {

    val obsLogActions = new ObsLogActions(odb)
    val query         = GsaRecordQuery(host, site)

    val syncUpdate: (List[GsaRecord], List[DatasetLabel]) => DmanAction[DatasetUpdates] = host match {
      case Summit(_)  => obsLogActions.updateSummit
      case Archive(_) => obsLogActions.updateArchive
    }

    val temporalUpdate: List[GsaRecord] => DmanAction[DatasetUpdates] = host match {
      case Summit(_)  => obsLogActions.updateSummit
      case Archive(_) => obsLogActions.updateArchive
    }

    override def program(pid: SPProgramID): DmanAction[DatasetUpdates] =
      idPoll(DmanId.Prog(pid))

    override def observation(oid: SPObservationID): DmanAction[DatasetUpdates] =
      idPoll(DmanId.Obs(oid))

    override def dataset(lab: DatasetLabel): DmanAction[DatasetUpdates] =
      idPoll(DmanId.Dset(lab))

    private def idPoll(did: DmanId): DmanAction[DatasetUpdates] = {
      def queryAction: DmanAction[List[GsaRecord]] =
        did match {
          case DmanId.Prog(pid) => query.program(pid).liftDman
          case DmanId.Obs(oid)  => query.observation(oid).liftDman
          case DmanId.Dset(lab) => query.dataset(lab).map(_.toList).liftDman
        }

      // Given a program, observation, or dataset and the set of all records in
      // the archive server, compute the set of labels corresponding to datasets
      // that aren't in the remote server.
      def missingLabels(p: ISPProgram, inGsa: List[GsaRecord]): List[DatasetLabel] = {
        def findObs(oid: SPObservationID): Option[ISPObservation] =
          p.getAllObservations.asScala.find(_.getObservationID == oid)

        def obsLabels(o: ISPObservation): Set[DatasetLabel] =
          (for {
            logNode <- Option(o.getObsExecLog)
            log     <- Option(logNode.getDataObject).collect {
                         case oel: ObsExecLog => oel
                       }
          } yield log.getRecord.getAllDatasetExecRecords.asScala.map(_.label).toSet) | Set.empty

        def diff(inProgram: Set[DatasetLabel]): List[DatasetLabel] =
          (inProgram &~ inGsa.flatMap(_.label).toSet).toList

        did match {
          case DmanId.Prog(_)   =>
            diff(p.getAllObservations.asScala.toSet.flatMap(obsLabels))

          case DmanId.Obs(oid)  =>
            findObs(oid).toList.flatMap { o => diff(obsLabels(o)) }

          case DmanId.Dset(lab) =>
            findObs(lab.getObservationId).toList.flatMap { o =>
              diff(obsLabels(o) & Set(lab))
            }
        }
      }

      for {
        recs  <- queryAction
        p     <- lookupProgram(did.pid, odb)
        ups   <- writeLocked(p.getNodeKey) {
          for {
            miss <- DmanAction(missingLabels(p, recs))
            ups  <- syncUpdate(recs, miss.toList)
          } yield ups
        }
      } yield ups
    }


    override val thisWeek: DmanAction[DatasetUpdates] = temporalPoll(_.thisWeek)
    override val tonight: DmanAction[DatasetUpdates]  = temporalPoll(_.tonight)

    private def temporalPoll(q: GsaRecordQuery => GsaResponse[List[GsaRecord]]): DmanAction[DatasetUpdates] =
      for {
        recs <- q(query).liftDman
        ups  <- temporalUpdate(recs)
      } yield ups

  }
}

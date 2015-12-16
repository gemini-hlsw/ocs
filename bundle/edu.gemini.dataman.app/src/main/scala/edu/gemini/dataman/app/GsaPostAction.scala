package edu.gemini.dataman.app

import edu.gemini.dataman.core._

import edu.gemini.gsa.query._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.dataset.QaRequestStatus.ProcessingPost
import edu.gemini.spModel.dataset.{DatasetLabel, DatasetExecRecord}
import edu.gemini.spModel.dataset.SummitState.ActiveRequest

import java.util.UUID

import scalaz._
import Scalaz._

/** A factory for the GSA server QA update request post.
  */
sealed trait GsaPostAction {
  /** Returns an action which when executed attempts to post QA requests to the
    * GSA, recording updates to corresponding dataset records as it progresses.
    */
  def postUpdate(labels: List[(DatasetLabel, UUID)]): DmanAction[DatasetUpdates]

}

object GsaPostAction {

  def apply(host: GsaHost.Summit, site: Site, auth: GsaAuth, odb: IDBDatabaseService): GsaPostAction = new GsaPostAction {
    val obsLogActions = new ObsLogActions(odb)
    val query         = GsaQaUpdateQuery(host, site, auth)

    override def postUpdate(labels: List[(DatasetLabel, UUID)]): DmanAction[DatasetUpdates] = {
      // Turn the GSA response into the type expected by SummitStateActions
      def results(reqs: List[(QaRequest, UUID)], res: GsaResponse[List[QaResponse]]): List[(DatasetLabel, UUID, String \/ Unit)] =
        res match {
          case -\/(err) =>
            // General failure, mark all with the same message.
            reqs.map { case (r,i) => (r.label, i, err.explain.left[Unit]) }
          case \/-(rs) =>
            // Individual results returned.  Pair them up with their IDs.
            val idMap   = reqs.map { case (r,i) => r.label -> i }.toMap
            val missing = (idMap.keySet &~ rs.map(_.label).toSet).map { label =>
              (label, idMap(label), s"No response from archive server for '$label'.".left[Unit])
            }

            missing.toList ++ rs.flatMap { r => idMap.get(r.label).map(i => (r.label, i, r.failure <\/(()))) }
        }

      obsLogActions.sendQaRequest(labels).flatMap { case (qaUpdates, exUpdates) =>

        // Partition and map at the same time.  We partition the exUpdates into
        // those that are processing and those that are not processing. For
        // the ones in the processing state, map them to (QaRequest, UUID).
        val (processing, notProcessing) = ((List.empty[(QaRequest, UUID)], List.empty[DatasetExecRecord])/:exUpdates) {
          case ((reqs, others), DatasetExecRecord(ds, ActiveRequest(_, qa, id0, ProcessingPost, _, _), _)) =>
            ((QaRequest(ds.getLabel, qa), id0) :: reqs, others)
          case ((recs, others), der) =>
            (recs, der :: others)
        }

        processing.isEmpty ? DmanAction((qaUpdates, notProcessing)) | {
          for {
            res  <- DmanAction(query.setQaStates(processing.unzip._1))
            ups2 <- obsLogActions.recordQaResult(results(processing, res))
          } yield (qaUpdates, notProcessing) |+| ups2
        }
      }
    }
  }
}
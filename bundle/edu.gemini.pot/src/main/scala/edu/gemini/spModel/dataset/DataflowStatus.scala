package edu.gemini.spModel.dataset

import edu.gemini.spModel.dataset.SummitState.{ActiveRequest, Idle, Missing}

import scala.Function.const
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/** Summarizes the status of a dataset in the dataflow system, focusing on
  * statuses important for a user and combining dataset availability with QA
  * state.
  */
sealed trait DataflowStatus {
  import DataflowStatus._

  /** Returns a description of the dataset disposition suitable for display. */
  def description: String = this match {
    case Unavailable      => "Missing Dataset"
    case Archived         => "Archived"
    case NeedsQa          => "Needs QA Evaluation"
    case SyncPending      => "QA Sync Pending"
    case CheckRequested   => "Needs CS Evaluation"
    case UpdateFailure    => "QA Update Error"
    case UpdateInProgress => "QA Update in Progress"
    case SummitOnly       => "Not in Public Archive"
    case Diverged         => "Awaiting Archive Sync"
    case InSync           => "In Sync with Archive"
  }
}

object DataflowStatus {

  /** The initial status, usually.  It means that we know about the dataset in
    * the OCS because we received an event from the seqexec but it has not been
    * confirmed by querying either the summit FITS server nor the public
    * archive.  This is the dreaded “datasets are gray” status.
    */
  case object Unavailable       extends DataflowStatus

  /** End of life state.  It means that the dataset is in the public archive but
    * no longer on the summit. Datasets in this state can no longer be QA’ed.
    * Whatever QA state they had before the dataset was deleted from dataflow
    * is the final QA state.
    */
  case object Archived          extends DataflowStatus

  /** Dataset "Undefined" QA state in the ODB.  It exists on the summit but may
    * or may not be in the public archive.
    */
  case object NeedsQa           extends DataflowStatus

  /** A QA state has been set locally and has not been synchronized with the
    * ODB or else has been recently synchronized and not yet sent to the GSA
    * since the OT last checked.
    */
  case object SyncPending       extends DataflowStatus

  /** Dataset has “Check” QA state.  It exists on the summit but may or may not
    * be in the public archive.
    */
  case object CheckRequested    extends DataflowStatus

  /** QA Update attempt failed.  It will retry automatically.
    */
  case object UpdateFailure     extends DataflowStatus

  /** There is an unsynchronized update to the QA state in the ODB that has not
    * yet been reflected in the FITS storage server, as of the last time we
    * polled for an update.  It may or may not be updated on disk.
    */
  case object UpdateInProgress  extends DataflowStatus

  /** The dataset is in sync with the database, but does not exist in the
    * archive.
    */
  case object SummitOnly        extends DataflowStatus

  /** Dataset versions differ between the summit and public archive.  Presumably
    * they will eventually be synchronized.
    */
  case object Diverged          extends DataflowStatus

  /** Dataset exists on the summit and the public archive, they are the same
    * and there are no pending updates from the user.  For datasets taken from
    * the last few semesters this should be the norm until the corresponding
    * file is removed from the "dataflow" directory (at which time it will
    * transition to `Archived`).
    */
  case object InSync            extends DataflowStatus

  /** @group Constructors */
  def unavailable: DataflowStatus      = Unavailable

  /** @group Constructors */
  def archived: DataflowStatus         = Archived

  /** @group Constructors */
  def needsQa: DataflowStatus          = NeedsQa

  /** @group Constructors */
  //noinspection MutatorLikeMethodIsParameterless
  def syncPending: DataflowStatus      = SyncPending

  /** @group Constructors */
  def checkRequested: DataflowStatus   = CheckRequested

  /** @group Constructors */
  //noinspection MutatorLikeMethodIsParameterless
  def updateFailure: DataflowStatus    = UpdateFailure

  /** @group Constructors */
  //noinspection MutatorLikeMethodIsParameterless
  def updateInProgress: DataflowStatus = UpdateInProgress

  /** @group Constructors */
  def summitOnly: DataflowStatus       = SummitOnly

  /** @group Constructors */
  def diverged: DataflowStatus         = Diverged

  /** @group Constructors */
  def inSync: DataflowStatus           = InSync


  // Order of attention urgency / priority that each option deserves.  When
  // summarizing a collection of datasets, we show the highest priority of the
  // states as *the* state that represents the group as a whole.
  val All: List[DataflowStatus] =
    List(
      unavailable,
      checkRequested,
      needsQa,
      syncPending,
      updateFailure,
      updateInProgress,
      summitOnly,
      diverged,
      inSync,
      archived
    )

  val AllJava: java.util.List[DataflowStatus] =
    All.asJava

  private val OrderMap = All.zipWithIndex.toMap

  /**
    * @group Typeclass Instances
    */
  implicit val OrderDatasetDisposition: Order[DataflowStatus] =
    Order.orderBy(OrderMap)

  /** Returns the `DataflowStatus` that should be associated with the
    * corresponding dataset.
    */
  def derive(rec: DatasetRecord): DataflowStatus = {
    import QaRequestStatus.Failed
    import DatasetQaState._
    def fromQa(gsa: DatasetGsaState): Option[DataflowStatus] = rec.qa.qaState match {
      case UNDEFINED          => needsQa.some
      case qa if qa != gsa.qa => syncPending.some
      case CHECK              => checkRequested.some
      case _                  => none
    }

    def archiveSync: DataflowStatus = rec.exec.archive.fold(summitOnly) { a =>
      rec.exec.summit.gsaMd5Option.exists(_ === a.md5) ? inSync | diverged
    }

    rec.exec.summit match {
      case Missing(_)                              =>
        rec.exec.archive.fold(unavailable)(const(archived))

      case Idle(gsa)                               =>
        fromQa(gsa) | archiveSync

      case ActiveRequest(_, _, _, Failed(_), _, _) =>
        updateFailure

      case _                                       =>
        updateInProgress
    }
  }

  /** Returns the highest priority status amongst all the datasets (if any). */
  def rollUp(recs: List[DatasetRecord]): Option[DataflowStatus] =
    recs.map(derive).minimum

  val NoData: String =
    "No Data"

  def description(s: Option[DataflowStatus]): String =
    s.fold(NoData)(_.description)
}
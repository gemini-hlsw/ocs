package edu.gemini.spModel.dataset

import edu.gemini.spModel.dataset.DatasetQaState.UNDEFINED
import edu.gemini.spModel.dataset.QaRequestStatus._
import edu.gemini.spModel.dataset.DatasetCodecs._
import edu.gemini.spModel.dataset.Implicits._
import edu.gemini.spModel.pio.{Pio, ParamSet}
import edu.gemini.spModel.pio.codec.{UnknownTag, MissingKey, PioError, ParamSetCodec}
import edu.gemini.spModel.pio.xml.PioXmlFactory

import java.time.Instant
import java.util.UUID

import scalaz._
import Scalaz._

/** Tracks dataset information at the summit.  There are three variables in play
  *
  * - '''gsa''' attributes of the dataset record in the summit GSA (if any).
  * This consists of the QA state, the time at which the last update to the
  * dataset was ingested, and an MD5 hash of the uncompressed dataset file.
  *
  * - '''request''' QA State as requested by the user.
  *
  * - Progress of a request through the system, recorded in instances of the
  * `SummitState` trait.
  *
  * The GSA record of the dataset can change at any time.  For example, a user
  * might open IRAF and edit the header of the dataset on disk. Any change to
  * the dataset file should eventually update the corresponding record in the
  * GSA.  We therefore poll for updates and record them.  This data is what we
  * refer to as the `DatasetGsaState`.
  *
  * The ODB records a dataset in the program model when it receives the
  * corresponding event from the seqexec.  A dataset may not yet have appeared
  * in the summit dataflow directory at this time, or it may have once been
  * present in the past but now is deleted.  Therefore the GSA dataset state is
  * not guaranteed to exist.  The `Missing` `SummitState` instance corresponds
  * to this case.
  *
  * When the user synchronizes a program, any edits to QA state are recognized
  * and `SummitState.updateRequest(current, newQaRequest)` is called to
  * calculate what the new `SummitState` should be in response. Depending on
  * the result, a request may be made to the summit GSA server to update the
  * QA state.
  *
  * QA state update requests to the GSA server are asynchronous. We get a
  * synchronous fail/accept response but the actual change happens at some
  * future time.  We cannot distinguish between the QA state changing in the GSA
  * server for some arbitrary reason vs. changing because of our request.  We
  * cannot track every change to a dataset in the server and instead only get
  * snapshots at poll intervals. Therefore while waiting for the asynchronous
  * request to complete, any update to the GSA dataset record is considered to
  * end the request.  Our request may eventually subsequently happen regardless,
  * or it may have already happened but has been overridden by an unrelated
  * edit to the dataset file.
  *
  * Finally, when a QA update generates an error response for whatever reason,
  * we record the error but automatically retry at some point in the future.
  */
sealed trait SummitState { self =>
  import SummitState._

  /** Returns the user requested QA state, which may or may not match what
    * is currently in the GSA record at the summit.
    */
  def request: DatasetQaState = self match {
    case Missing(r)                      => r
    case Idle(g)                         => g.qa
    case ActiveRequest(_, r, _, _, _, _) => r
  }

  /** Returns the last recorded poll data (if any) for the GSA record associated
    * with this dataset at the summit.
    */
  def gsaOption: Option[DatasetGsaState] = self match {
    case Missing(_)                      => None
    case Idle(g)                         => Some(g)
    case ActiveRequest(g, _, _, _, _, _) => Some(g)
  }

  def isAvailable: Boolean = gsaOption.isDefined

  /** Returns the QA state as recorded in the GSA record associated with this
    * dataset (if any.).
    */
  def gsaQaStateOption: Option[DatasetQaState] = gsaOption.map(_.qa)

  /** Returns the last modification time of the GSA dataset record associated
    * with this dataset (if any).
    */
  def gsaTimestampOption: Option[Instant]      = gsaOption.map(_.timestamp)

  /** Returns the MD5 of the dataset file as it exists in the GSA. */
  def gsaMd5Option: Option[DatasetMd5]         = gsaOption.map(_.md5)

  /** Gets the active request id, if applicable. */
  def requestId: Option[UUID] = self match {
    case ActiveRequest(_, _, uid, _, _, _) => Some(uid)
    case _                                 => None
  }

  object transition extends Serializable {

    import QaRequestStatus._

    /** Computes the new `SummitState` that should apply after recording the
      * given request to update the QA value.
      *
      * Note that if we are already in the process of sending an asynchronous QA
      * state update request to the summit server, we will abandon that request
      * and start over.
      */
    def userRequest(newQa: DatasetQaState): SummitState = self match {
      case Missing(_)                                 =>
        Missing(newQa)

      case Idle(g)                                    =>
        (newQa === g.qa) ? self | pendingPost(g, newQa)

      case ActiveRequest(g, r, _, _, _, _)            =>
        (newQa === g.qa) ? idle(g) | pendingPost(g, newQa)
    }

    /** Computes the new `SummitState` that should apply after recording the
      * latest poll result from the summit.
      *
      * If the GSA state was previously missing but has now been discovered, and
      * if the dataset information it records shows that it does not yet have a
      * defined QA state, we apply the user request.  This allows the user to set
      * QA states while still waiting for datasets to show up on the summit.  On
      * the other hand, if the QA state initially appears in a defined state, we
      * respect it and ignore whatever request the user may have made.
      *
      * Once a request to update QA state is sent to the GSA, any update to GSA
      * state is considered to cancel/fulfill our request regardless of the
      * QA state it contains.  Updates to QA state can happen spontaneously from
      * the point of view of the Data Manager and we cannot know whether any
      * ongoing request completed before or perhaps will complete afterwords.
      */
    def gsaState(newGsaOpt: Option[DatasetGsaState]): SummitState =
      newGsaOpt.fold(missing(request)) { ng /* new gsa state */ =>
        self match {
          case Missing(r) =>
            ((r === ng.qa) || (ng.qa =/= UNDEFINED)) ? idle(ng) | pendingPost(ng, r)

          case _          =>
            gsaOption.forall(cur => ng.isAfter(cur)) ? idle(ng) | self
        }
      }

    private def qaUpdate(id: UUID, expected: QaRequestStatus, next: QaRequestStatus): SummitState = self match {
      case ar@ActiveRequest(_, _, `id`, `expected`, _, _)  => ar.updated(next)
      case _                                               => self
    }

    /** Marks a `PendingPost` dataset as `ProcessingPost`, which is done just
      * before sending a QA request to the FITS storage server.
      */
    def pendingToProcessing(id: UUID): SummitState =
      qaUpdate(id, PendingPost, ProcessingPost)

    /** Marks an outstanding QA request in status `ProcessingPost` as `Failed`.
      */
    def processingToFailed(id: UUID, msg: String): SummitState =
      qaUpdate(id, ProcessingPost, Failed(msg))

    /** Marks an outstanding QA request in status `ProcessingPost`as `Accepted`.
      */
    def processingToAccepted(id: UUID): SummitState =
      qaUpdate(id, ProcessingPost, Accepted)

    /** Marks any active request as `PendingPost`. This transition is used when
      * failed QA requests are reset to be retried.
      */
    def activeToPending(id: UUID): SummitState = self match {
      case ar@ActiveRequest(_, _, `id`, _, _, _)   => ar.reset
      case _                                       => self
    }

    /** Marks any active request as `Failed`. This transition is used at startup
      * to sweep any outstanding QA update requests to a failure state to be
      * eventually retried.
      */
    def activeToFailed(id: UUID, msg: String): SummitState = self match {
      case ar@ActiveRequest(_, _, `id`, _, _, _) => ar.updated(Failed(msg))
      case _                                     => self
    }

    /**
      */
    def pendingSyncToFailed(qa: DatasetQaState, msg: String): SummitState = self match {
      case Idle(gsa) => ActiveRequest(gsa, qa, UUID.randomUUID(), Failed(msg), Instant.now(), 1)
      case _         => self
    }
  }
}

object SummitState {

  /** No information for the dataset in the summit GSA server.
    *
    * @param req user requested QA state
    */
  final case class Missing(req: DatasetQaState) extends SummitState

  object Missing {
    val empty = Missing(DatasetQaState.UNDEFINED)

    val req: Missing @> DatasetQaState = Lens.lensu((a, b) => a.copy(req = b), _.req)

    implicit val ParamSetCodecMissing: ParamSetCodec[Missing] =
      ParamSetCodec.initial(empty).withParam("req", req)
  }

  /** No expected changes as a result of user QA state changes.  The QA state
    * in the GSA record should be applied to the program model so that they
    * match.
    *
    * @param gsa state of the dataset in the summit GSA server
    */
  final case class Idle(gsa: DatasetGsaState) extends SummitState

  object Idle {
    val empty = Idle(DatasetGsaState.empty)

    val gsa: Idle @> DatasetGsaState = Lens.lensu((a, b) => a.copy(gsa = b), _.gsa)

    implicit val ParamSetCodecIdle: ParamSetCodec[Idle] =
      ParamSetCodec.initial(empty).withParamSet("gsa", gsa)
  }

  /** A QA update request has been made but there is as of yet no evidence of
    * completion.  This state is further partitioned into the various stages
    * of processing a request as detailed in `QaRequestStatus`.
    *
    * @param gsa state of the dataset in the summit GSA server
    * @param req user requested QA state
    * @param id unique id used to distinguish transitions for distinct requests;
    *           the transition from pending -> processing -> accepted/failed
    *           is automated and involves remote communication so this is used
    *           to ensure that the dataset has not been reset in the interim
    * @param status progress of the request
    * @param when the time (according to the ODB) that the status last changed
    * @param retryCount how many failure attempts have been retried
    */
  final case class ActiveRequest(
                     gsa: DatasetGsaState,
                     req: DatasetQaState,
                     id: UUID,
                     status: QaRequestStatus,
                     when: Instant,
                     retryCount: Int) extends SummitState {

    def updated(s: QaRequestStatus): ActiveRequest =
      copy(status = s, when = Instant.now())

    def reset: ActiveRequest =
      ActiveRequest(gsa, req, UUID.randomUUID(), PendingPost, Instant.now(), retryCount + 1)
  }

  object ActiveRequest {
    val gsa:        ActiveRequest @> DatasetGsaState = Lens.lensu((a, b) => a.copy(gsa = b),        _.gsa)
    val req:        ActiveRequest @> DatasetQaState  = Lens.lensu((a, b) => a.copy(req = b),        _.req)
    val id:         ActiveRequest @> UUID            = Lens.lensu((a, b) => a.copy(id = b),         _.id)
    val status:     ActiveRequest @> QaRequestStatus = Lens.lensu((a, b) => a.copy(status = b),     _.status)
    val when:       ActiveRequest @> Instant         = Lens.lensu((a, b) => a.copy(when = b),       _.when)
    val retryCount: ActiveRequest @> Int             = Lens.lensu((a, b) => a.copy(retryCount = b), _.retryCount)

    // An "empty" element for the purposes of using the ParamSetCodec DSL.
    val empty = ActiveRequest(
      DatasetGsaState.empty,
      DatasetQaState.UNDEFINED,
      new UUID(0L, 0L),
      QaRequestStatus.Failed(""),
      Instant.ofEpochMilli(0),
      0
    )

    implicit val ParamSetCodecActiveRequest: ParamSetCodec[ActiveRequest] =
      ParamSetCodec.initial(empty)
        .withParamSet("gsa", gsa)
        .withParam("req", req)
        .withParam("id", id)(ParamCodecUuid)
        .withParamSet("status", status)
        .withParam("when", when)(ParamCodecInstant)
        .withParam("retry", retryCount)
  }

  /** Constructs a `Missing` instance with static type `SummitState`, which is
    * convenient for folds, etc.
    *
    * @group Constructors
    */
  def missing(req: DatasetQaState): SummitState = Missing(req)

  /** Constructs an `Idle` instance with static type `SummitState`, which is
    * convenient for folds, etc.
    *
    * @group Constructors
    */
  def idle(gsa: DatasetGsaState): SummitState =
    Idle(gsa)

  def pendingPost(gsa: DatasetGsaState, req: DatasetQaState): SummitState =
    ActiveRequest(gsa, req, UUID.randomUUID(), PendingPost, Instant.now(), 0)

  /**
   * @group Typeclass Instances
   */
  implicit val EqualSummitState: Equal[SummitState] = Equal.equalA

  implicit val ParamSetCodecSummitState: ParamSetCodec[SummitState] =
    new ParamSetCodec[SummitState] {
      val pf = new PioXmlFactory

      def encode(key: String, a: SummitState): ParamSet = {
        val (tag, ps) = a match {
          case m: Missing       => ("missing", Missing.ParamSetCodecMissing.encode(key, m))
          case i: Idle          => ("idle",    Idle.ParamSetCodecIdle.encode(key, i))
          case a: ActiveRequest => ("active",  ActiveRequest.ParamSetCodecActiveRequest.encode(key, a))
        }
        Pio.addParam(pf, ps, "tag", tag)
        ps
      }

      def decode(ps: ParamSet): PioError \/ SummitState =
        Option(ps.getParam("tag")).map(_.getValue) \/> MissingKey("tag") flatMap {
          case "missing" => Missing.ParamSetCodecMissing.decode(ps)
          case "idle"    => Idle.ParamSetCodecIdle.decode(ps)
          case "active"  => ActiveRequest.ParamSetCodecActiveRequest.decode(ps)
          case bah       => UnknownTag(bah, "SummitState").left
        }
    }
}

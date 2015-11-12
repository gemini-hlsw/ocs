package edu.gemini.spModel.dataset

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.dataset.DatasetCodecs._
import edu.gemini.spModel.dataset.SummitState.{ActiveRequest, Idle, Missing}
import edu.gemini.spModel.pio.codec.ParamSetCodec
import edu.gemini.spModel.pio.{PioFactory, ParamSet}

import Function.const

import scalaz._
import Scalaz._

import DatasetExecRecord._

final case class DatasetExecRecord(
  dataset: Dataset,
  summit: SummitState,
  archive: Option[DatasetGsaState]) {

  def label: DatasetLabel = dataset.getLabel

  def dataflowStatus: DataflowStatus = {
    import DataflowStatus._

    def fromQa: Option[DataflowStatus] = summit.request match {
      case DatasetQaState.CHECK     => checkRequested.some
      case DatasetQaState.UNDEFINED => needsQa.some
      case _                        => none
    }

    def archiveSync: DataflowStatus = archive.fold(summitOnly) { a =>
      summit.gsaMd5Option.exists(_ === a.md5) ? inSync | diverged
    }

    summit match {
      case Missing(_) => archive.fold(unavailable)(const(archived))
      case Idle(_)    => fromQa | archiveSync
      case _          => fromQa | updateInProgress
    }
  }

  def pset(f: PioFactory): ParamSet =
    ParamSetCodecDatasetExecRecord.encode(DatasetExecRecord.ParamSet, this)
}

object DatasetExecRecord {
  val ParamSet = "datasetRecord"

  def apply(dataset: Dataset): DatasetExecRecord =
    DatasetExecRecord(dataset, SummitState.missing(DatasetQaState.UNDEFINED), None)

  def apply(pset: ParamSet): DatasetExecRecord =
    unsafeDecode[DatasetExecRecord](pset)

  val dataset: DatasetExecRecord @> Dataset                 = Lens.lensu((a, b) => a.copy(dataset = b), _.dataset)
  val summit:  DatasetExecRecord @> SummitState             = Lens.lensu((a, b) => a.copy(summit = b), _.summit)
  val archive: DatasetExecRecord @> Option[DatasetGsaState] = Lens.lensu((a, b) => a.copy(archive = b), _.archive)

  def ssPlens[A <: SummitState : Manifest]: DatasetExecRecord @?> A = PLens.plensg(der => Some(ss => der.copy(summit = ss)), {
    case DatasetExecRecord(_, ss: A, _) => some(ss)
    case _                              => none
  })

  val missing: DatasetExecRecord @?> Missing       = ssPlens
  val idle:    DatasetExecRecord @?> Idle          = ssPlens
  val active:  DatasetExecRecord @?> ActiveRequest = ssPlens

  implicit val EqualDatasetExecRecord: Equal[DatasetExecRecord] = Equal.equalA

  private val empty = DatasetExecRecord(
    new Dataset(new DatasetLabel(new SPObservationID(SPProgramID.toProgramID("GN-0000A-C-0"), 0), 0), "", 0L),
    SummitState.Missing.empty,
    None
  )

  implicit val ParamSetCodecDatasetExecRecord: ParamSetCodec[DatasetExecRecord] =
    ParamSetCodec.initial(empty)
      .withParamSet("dataset", dataset)
      .withParamSet("summit", summit)
      .withOptionalParamSet("archive", archive)
}



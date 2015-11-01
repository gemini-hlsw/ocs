package edu.gemini.spModel.dataset

import edu.gemini.spModel.dataset.DatasetCodecs._
import edu.gemini.spModel.dataset.SummitState.{ActiveRequest, Idle, Missing}
import edu.gemini.spModel.pio.codec.{PioError, ParamSetCodec}
import edu.gemini.spModel.pio.xml.PioXmlFactory
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

  implicit val ParamSetCodecDatasetExecRecord =
    new ParamSetCodec[DatasetExecRecord]() {
      val pf = new PioXmlFactory

      override def encode(key: String, a: DatasetExecRecord): ParamSet =
        pf.createParamSet(key)
          .withParamSet("dataset", a.dataset)
          .withParamSet("summit", a.summit)
          .withOptionalParamSet("archive", a.archive)

      override def decode(ps: ParamSet): PioError \/ DatasetExecRecord =
        for {
          dataset <- decodeParamSet[Dataset]("dataset", ps)
          summit  <- decodeParamSet[SummitState]("summit", ps)
          archive <- decodeOptionalParamSet[DatasetGsaState]("archive", ps)
        } yield DatasetExecRecord(dataset, summit, archive)
  }
}



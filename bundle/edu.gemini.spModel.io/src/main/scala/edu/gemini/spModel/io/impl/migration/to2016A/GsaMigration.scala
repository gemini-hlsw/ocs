package edu.gemini.spModel.io.impl.migration.to2016A

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.dataset.{SummitState, DatasetQaState, DatasetLabel}
import edu.gemini.spModel.io.PioSyntax
import edu.gemini.spModel.pio.codec.ParamSetCodec
import edu.gemini.spModel.pio.{Pio, ParamSet, Container, Document}

import scalaz._
import Scalaz._

/** Migrates DatasetExecRecord ParamSets for 2016A, marking the datasets as
  * Missing, pending the initial sync with the new archive.
  */
object GsaMigration {

  import PioSyntax._

  private final case class OldExecRecord(ps: ParamSet, qaState: DatasetQaState) {
    def convert(): Unit =
      if (Option(ps.getParamSet("summit")).isEmpty) {
        ps.removeChild("syncTime")
        ps.removeChild("datasetFileState")
        ps.removeChild("gsaState")
        ps.addParamSet(
          ParamSetCodec[SummitState].encode("summit", SummitState.missing(qaState))
        )
      }
  }

  private final case class ObsLogContainers(exec: Container, qa: Option[Container]) {
    def records: List[OldExecRecord] = {
      val qaRecords = qa.toList.flatMap(_.allParamSets.filter(_.getName === "datasetQa"))
      val qaMap     = qaRecords.flatMap { qa =>
        for {
          ls <- Option(Pio.getValue(qa, "label"))
          l  <- \/.fromTryCatchNonFatal(new DatasetLabel(ls)).toOption
          qs <- Option(Pio.getValue(qa, "qaState"))
          q  <- Option(DatasetQaState.parseType(qs))
        } yield l -> q
      }.toMap

      val execRecords  = exec.allParamSets.filter(_.getName === "datasetRecord")
      execRecords.flatMap { ps =>
        for {
          ls <- Option(Pio.getValue(ps, "dataset/datasetLabel"))
          l  <- \/.fromTryCatchNonFatal(new DatasetLabel(ls)).toOption
        } yield OldExecRecord(ps, qaMap.getOrElse(l, DatasetQaState.UNDEFINED))
      }
    }
  }

  private def obsLog(obs: Container): Option[ObsLogContainers] =
    obs.findContainers(SPComponentType.OBS_EXEC_LOG).headOption.map { exec =>
      new ObsLogContainers(exec, obs.findContainers(SPComponentType.OBS_QA_LOG).headOption)
    }

  private def allDatasetExecRecords(d: Document): List[OldExecRecord] =
    for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      olc <- obsLog(obs).toList
      rec <- olc.records
    } yield rec

  def convertDatasetRecords(d: Document): Unit =
    allDatasetExecRecords(d).foreach(_.convert())
}

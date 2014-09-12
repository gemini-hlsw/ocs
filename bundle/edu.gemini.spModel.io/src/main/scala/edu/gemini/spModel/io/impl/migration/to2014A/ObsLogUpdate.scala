package edu.gemini.spModel.io.impl.migration.to2014A

import edu.gemini.pot.sp.{SPNodeKey, ISPFactory, ISPObservation}

import edu.gemini.spModel.dataset.{DatasetQaState, DatasetQaRecord, DatasetLabel}
import edu.gemini.spModel.obs.{SPObservation, ObservationStatus}
import edu.gemini.spModel.obslog.{ObsExecLog, ObsQaLog}
import edu.gemini.spModel.obsrecord.{ObsExecStatus, ObsQaRecord}
import edu.gemini.spModel.pio.{Pio, ParamSet, Container}

import java.text.ParseException
import java.util.logging.Logger
import java.util.UUID

import scala.collection.JavaConverters._

/**
 * Handles parsing the new obs logs out of the old observing log if necessary.
 * This is the data migration for observing log for 2013B.
 */
private[to2014A] object ObsLogUpdate {
  private val Log = Logger.getLogger(ObsLogUpdate.getClass.getName)

  def update(f: ISPFactory, obs: ISPObservation, c: Container): Unit = {
    // Gets a node key with all the bits flipped except the ones that mark
    // it as a random uuid.
    def flip(uuid: UUID): UUID = {
      val m = uuid.getMostSignificantBits  ^ 0xFFFFFFFFFFFF0FFFl
      val l = uuid.getLeastSignificantBits ^ 0xFFFFFFFFFFFFFFFFl
      new UUID(m, l)
    }
    def flipKey(key: SPNodeKey) = new SPNodeKey(flip(key.uuid))

    // Use the old obs comp obslog key for the exec log and make a new uuid
    // by flipping the random bits of the key for the qa log.  We want to have
    // a reproduce-able key for both new nodes.
    obsLogParamSet(c) foreach { case (ps, key) =>
      val qaNode = f.createObsQaLog(obs.getProgram, flipKey(key))
      qaNode.setDataObject(parseQa(ps))
      obs.setObsQaLog(qaNode)

      val execNode = f.createObsExecLog(obs.getProgram, key)
      execNode.setDataObject(parseExec(ps))
      obs.setObsExecLog(execNode)
    }

    // Look for the "status" param in the observation and turn it into
    // the two status values.  Directly set the ObsPhase2Status from this and
    // compare the computed exec status to the value we come up with here.
    // If they are the same, do nothing.  If they differ, set the override
    // flag to the value we computed.
    obsStatus(c) foreach { os =>
      val p2 = os.phase2()
      val ob = obs.getDataObject.asInstanceOf[SPObservation]
      ob.setPhase2Status(p2)
      obs.setDataObject(ob)

      if (os != ObservationStatus.computeFor(obs)) {
        val over = os match {
          case ObservationStatus.ONGOING  => ObsExecStatus.ONGOING
          case ObservationStatus.OBSERVED => ObsExecStatus.OBSERVED
          case _                          => ObsExecStatus.PENDING
        }
        Log.info(s"Data Migration: override exec status: ${obs.getObservationID} = $over")
        ob.setExecStatusOverride(new edu.gemini.shared.util.immutable.Some[ObsExecStatus](over))
        obs.setDataObject(ob)
      }
    }
  }

  private def obsDataObject(c: Container): Option[ParamSet] =
    (c.getParamSets.asScala collect {
        case ps: ParamSet if ps.getKind == "dataObj" => ps
    }).headOption

  private def obsStatus(c: Container): Option[ObservationStatus] =
    for {
      ps <- obsDataObject(c)
      s  <- Option(Pio.getValue(ps, "status"))
    } yield ObservationStatus.getObservationStatus(s)

  private def obsLogParamSet(c: Container): Option[(ParamSet, SPNodeKey)] =
    for {
      co <- Option(c.getContainer("Observation Log"))
      p  <- Option(co.getParamSet("Observation Log"))
    } yield (p, new SPNodeKey(co.getKey))

  private def parseQa(ps: ParamSet): ObsQaLog =
    new ObsQaLog(new ObsQaRecord((for {
      dr <- datasetRecords(ps)
      e  <- toEntry(dr)
    } yield e).toMap.asJava))

  private def datasetRecords(ps: ParamSet): List[ParamSet] =
    (for {
      p1 <- Option(ps.getParamSet("obsRecord"))
      p2 <- Option(p1.getParamSet("datasets"))
    } yield p2.getParamSets("datasetRecord").asScala.toList).getOrElse(Nil)

  private def toEntry(p: ParamSet): Option[(DatasetLabel, DatasetQaRecord)] = {
    val q = Pio.getEnumValue[DatasetQaState](p, "qaState", DatasetQaState.UNDEFINED)
    val c = Pio.getValue(p, "comment", "")

    for {
      dp <- Option(p.getParamSet("dataset"))
      ls <- Option(Pio.getValue(dp, "datasetLabel"))
      l  <- toDatasetLabel(ls)
    } yield (l, new DatasetQaRecord(l,q,c))
  }

  private def toDatasetLabel(s: String): Option[DatasetLabel] =
    try {
      Some(new DatasetLabel(s))
    } catch {
      case _: ParseException => None
    }

  private def parseExec(ps: ParamSet): ObsExecLog = {
    val e = new ObsExecLog
    e.setParamSet(ps)
    e
  }
}

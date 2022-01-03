package edu.gemini.spModel.obsrecord

import edu.gemini.spModel.dataset._
import edu.gemini.spModel.pio.{ParamSet, PioFactory}

import scala.collection.JavaConverters._

object ObsQaRecord {
  val PARAM_SET = "obsQaRecord"

  def fromParamSet(ps: ParamSet): ObsQaRecord = {
    val m = (Map.empty[DatasetLabel, DatasetQaRecord]/:ps.getParamSets(DatasetQaRecord.PARAM_SET).asScala) { (m,p) =>
      val d = new DatasetQaRecord(p)
      m.updated(d.label, d)
    }
    ObsQaRecord(m)
  }
}

import ObsQaRecord._

/**
 * The ObsQaRecord is an immutable Map[DatasetLabel, DatasetQaRecord] with a
 * default empty DatasetQaRecord for any given dataset label.  This class is
 * needed to perform various operations in the ObsExecRecord which require
 * knowledge of the QA state of the datasets.
 */
case class ObsQaRecord(qaMap: Map[DatasetLabel, DatasetQaRecord]) {

  // for Java :/
  def this() = this(Map.empty[DatasetLabel, DatasetQaRecord])
  def this(m: java.util.Map[DatasetLabel, DatasetQaRecord]) = this(m.asScala.toMap)

  def apply(l: DatasetLabel): DatasetQaRecord = qaMap.getOrElse(l, DatasetQaRecord.empty(l))

  def updated(r: DatasetQaRecord): ObsQaRecord =
    if (qaMap.get(r.label).contains(r)) this else new ObsQaRecord(qaMap.updated(r.label, r))

  def comment(l: DatasetLabel): String         = apply(l).comment
  def qaState(l: DatasetLabel): DatasetQaState = apply(l).qaState

  def paramSet(f: PioFactory): ParamSet = {
    val ps = f.createParamSet(PARAM_SET)
    qaMap.values foreach { d => ps.addParamSet(d.toParamSet(f)) }
    ps
  }

  def datasetRecordsFromJava(ds: java.util.Collection[DatasetExecRecord]): List[DatasetRecord] =
    (ds.asScala:\List.empty[DatasetRecord]) { (r,l) =>
      new DatasetRecord(apply(r.label), r) :: l
    }

  def datasetRecordsJava(ds: java.util.Collection[DatasetExecRecord]): java.util.List[DatasetRecord] =
    datasetRecordsFromJava(ds).asJava

  // Merges the two qa records unless there are conflicting edits.
  def merge(that: ObsQaRecord): Option[ObsQaRecord] = {
    val mergedMap = (Option(qaMap)/:that.qaMap) { case (mO, (lab, thatRec)) =>
      mO.flatMap { m =>
        val rec = m.get(lab).fold(Option(thatRec)) { thisRec =>
          // Compare two values, if the same or one or the other is not zero,
          // return the defined value wrapped in a Some.  Otherwise, None.
          def nz[T](f: DatasetQaRecord => T, r0: DatasetQaRecord, r1: DatasetQaRecord, z: T): Option[T] = {
            val t0 = f(r0)
            val t1 = f(r1)
            if ((t0 == t1) || (t1 == z)) Some(t0)
            else if (t0 == z) Some(t1)
            else None  // both non-zero but not the same value
          }
          for {
            state   <- nz(_.qaState, thisRec, thatRec, DatasetQaState.UNDEFINED)
            comment <- nz(_.comment, thisRec, thatRec, "")
          } yield new DatasetQaRecord(lab, state, comment)
        }
        rec.map(r => m.updated(lab, r))
      }
    }
    mergedMap.map(ObsQaRecord(_))
  }
}

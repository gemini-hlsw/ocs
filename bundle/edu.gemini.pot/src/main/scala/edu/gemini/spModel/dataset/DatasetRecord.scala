package edu.gemini.spModel.dataset

import scalaz._
import Scalaz._

/** Combination `DatasetQaRecord` and `DatasetExecRecord`.  These are the
  * user-editable and server-updated (respectively) bits of a dataset.
  */
final case class DatasetRecord(qa: DatasetQaRecord, exec: DatasetExecRecord) {

  def label: DatasetLabel = qa.label

  // This is code converted from Java in order to get an extractor that matches
  // the name of the class.  This is poor but these tests were being applied
  // in the Java code and need to be reworked into the model to prevent these
  // situations.

  import Implicits._
  assert(qa   != null, "Cannot construct a DatasetRecord with a null DatasetQaRecord")
  assert(exec != null, "Cannot construct a DatasetRecord with a null DatasetExecRecord")
  assert(qa.label === exec.label, s"QA and Exec records for different datasets: ${qa.label} vs ${exec.label}")

  // Used by Java code.
  def withQa(qa: DatasetQaRecord): DatasetRecord       = copy(qa = qa)
  def withExec(exec: DatasetExecRecord): DatasetRecord = copy(exec = exec)
}

object DatasetRecord {
  implicit val EqualDatasetRecord: Equal[DatasetRecord] = Equal.equalA
}

package edu.gemini.spModel.dataset

import java.time.Instant

import scalaz._


/** Relevant dataset attributes tracked in the corresponding GSA records.
 *
 * @param qa qa state as it existed the last time that the server was polled
 * @param timestamp last modification time stamp of the dataset in the server
 * @param md5 hash of the version of the dataset file in the server
 */
case class DatasetGsaState(qa: DatasetQaState, timestamp: Instant, md5: DatasetMd5) {
  def isAfter(that: DatasetGsaState): Boolean  = timestamp.isAfter(that.timestamp)

  def isBefore(that: DatasetGsaState): Boolean = timestamp.isBefore(that.timestamp)
}

object DatasetGsaState {
  implicit val EqualDatasetGsaState: Equal[DatasetGsaState] = Equal.equalA
}

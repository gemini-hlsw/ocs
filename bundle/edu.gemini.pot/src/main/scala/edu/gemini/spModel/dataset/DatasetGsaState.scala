package edu.gemini.spModel.dataset

import edu.gemini.spModel.dataset.DatasetCodecs._
import edu.gemini.spModel.pio.codec.ParamSetCodec

import java.time.Instant

import scalaz._
import Scalaz._

/** Relevant dataset attributes tracked in the corresponding GSA records.
 *
 * @param qa qa state as it existed the last time that the server was polled
 * @param timestamp last modification time stamp of the dataset in the server
 * @param md5 hash of the version of the dataset file in the server
 */
final case class DatasetGsaState(qa: DatasetQaState, timestamp: Instant, md5: DatasetMd5) {
  def isAfter(that: DatasetGsaState): Boolean  = timestamp.isAfter(that.timestamp)

  def isBefore(that: DatasetGsaState): Boolean = timestamp.isBefore(that.timestamp)
}

object DatasetGsaState {

  val empty = DatasetGsaState(DatasetQaState.UNDEFINED, Instant.EPOCH, DatasetMd5.empty)

  val qa:        DatasetGsaState @> DatasetQaState = Lens.lensu((a, b) => a.copy(qa = b), _.qa)
  val timestamp: DatasetGsaState @> Instant        = Lens.lensu((a, b) => a.copy(timestamp = b), _.timestamp)
  val md5:       DatasetGsaState @> DatasetMd5     = Lens.lensu((a, b) => a.copy(md5 = b), _.md5)

  implicit val EqualDatasetGsaState: Equal[DatasetGsaState] = Equal.equalA

  implicit val ParamSetCodecDatasetGsaState: ParamSetCodec[DatasetGsaState] =
    ParamSetCodec.initial(empty)
      .withParam("qa",        qa)
      .withParam("timestamp", timestamp)(ParamCodecInstant)
      .withParam("md5",       md5)

}

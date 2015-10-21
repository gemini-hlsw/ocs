package edu.gemini.spModel.dataset

import javax.xml.bind.DatatypeConverter

import scalaz._

/** Provides an efficiently encoded data type for MD5 strings. */
final class DatasetMd5(val bytes: Array[Byte]) {
  override def equals(o: Any): Boolean =
    o match {
      case that: DatasetMd5 =>
        (bytes.length == that.bytes.length) && bytes.zip(that.bytes).forall {
          case (b0, b1) => b0 == b1
        }
      case _                => false
    }

  override def hashCode: Int =
    (41/:bytes) { (i,b) => 41 * (i + b) }

  def hexString: String = DatatypeConverter.printHexBinary(bytes)

  override def toString: String =
    s"DatasetMd5($hexString)"
}

object DatasetMd5 {
  def parse(hexString: String): Option[DatasetMd5] =
    \/.fromTryCatch {
      new DatasetMd5(DatatypeConverter.parseHexBinary(hexString))
    }.toOption

  implicit val EqualDatasetMd5: Equal[DatasetMd5] = Equal.equalA
  implicit val ShowDatasetMd5: Show[DatasetMd5]   = Show.shows(_.hexString)
}

package edu.gemini.ictd

import edu.gemini.spModel.core.{ ProgramId, Site }
import edu.gemini.spModel.ictd.Availability

import scala.collection.immutable.TreeMap

import scalaz._
import Scalaz._


/** The custom mask key identifies a custom mask.
  *
  * @param id    program id; ICTD only tracks ids that conform to the
  *              ProgramId.Science pattern
  * @param index running index, which must be positive
  */
sealed abstract case class CustomMaskKey(id: ProgramId.Science, index: Int) {

  assert(index >= 0, s"Invariant violated. $index is negative.")

  def format: String =
    f"${id.siteVal.abbreviation}${id.semesterVal.format}${id.ptypeVal.abbreviation}${id.index}-$index%02d"

}

object CustomMaskKey {

  def fromIdAndIndex(id: ProgramId.Science, index: Int): Option[CustomMaskKey] =
    (index >= 0) option new CustomMaskKey(id, index) {}

  def unsafeFromIdAndIndex(id: ProgramId.Science, index: Int): CustomMaskKey =
    fromIdAndIndex(id, index).getOrElse(sys.error(s"Cannot create CustomMaskKey with negative index: id=$id, index=$index"))

  private val MaskDef = """(G[NS])(\d\d\d\d)([AB])([A-Z]+)(\d+)-(\d+)""".r

  def parse(s: String): Option[CustomMaskKey] =
    s match {
      case MaskDef(site, year, ab, t, pidx, midx) =>
        for {
          pid <- ProgramId.parseScienceId(s"$site-$year$ab-$t-$pidx")
          key <- CustomMaskKey.fromIdAndIndex(pid, midx.toInt)
        } yield key
      case _                                      =>
        None
    }

  def unsafeParse(s: String): CustomMaskKey =
    parse(s).getOrElse(sys.error(s"Could not parse $s as a CustomMaskKey"))

  implicit val OrderingCustomMaskKey: scala.math.Ordering[CustomMaskKey] =
    scala.math.Ordering.by(k => (k.id.siteVal, k.id.semesterVal, k.id.ptypeVal, k.id.index, k.index))

  implicit val OrderCustomMaskKey: Order[CustomMaskKey] =
    Order.fromScalaOrdering

}
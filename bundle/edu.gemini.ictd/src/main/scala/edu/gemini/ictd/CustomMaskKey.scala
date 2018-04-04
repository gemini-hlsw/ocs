package edu.gemini.ictd

import edu.gemini.spModel.core.{ ProgramId, Site }
import edu.gemini.spModel.ictd.Availability

import scala.collection.immutable.TreeMap

import scalaz._
import Scalaz._


/** The custom mask key identifies a custom mask.
  *
  * @param id   program id; ICTD only tracks ids that conform to the
  *             ProgramId.Science pattern
  * @param name name of the custom mask definition file, which should match the
  *             value in the MOS ICTD table (sans the "_ODF.fits" suffix).
  */
final case class CustomMaskKey(id: ProgramId.Science, name: String)

object CustomMaskKey {

  implicit val OrderingCustomMaskKey: scala.math.Ordering[CustomMaskKey] =
    scala.math.Ordering.by(k => (k.id.siteVal, k.id.semesterVal, k.id.ptypeVal, k.id.index, k.name))

  implicit val OrderCustomMaskKey: Order[CustomMaskKey] =
    Order.fromScalaOrdering

}
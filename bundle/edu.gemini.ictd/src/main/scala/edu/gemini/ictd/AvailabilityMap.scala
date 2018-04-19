package edu.gemini.ictd

import edu.gemini.spModel.ictd.Availability

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/** AvailabilityMap wraps a Map[Enum[_], Availability] to give it a Monoid
  * instance and facilitate creating a java.util.Map.
  */
final case class AvailabilityMap(enumMap: Map[java.lang.Enum[_], Availability]) {

  def asJava: java.util.Map[java.lang.Enum[_], Availability] =
    enumMap.asJava

}

object AvailabilityMap {

  val empty: AvailabilityMap =
    AvailabilityMap(Map.empty)

  implicit val MonoidAvailabilityMap: Monoid[AvailabilityMap] =
    new Monoid[AvailabilityMap] {
      def zero: AvailabilityMap =
        AvailabilityMap.empty

      def append(a0: AvailabilityMap, a1: => AvailabilityMap): AvailabilityMap =
        AvailabilityMap(a0.enumMap ++ a1.enumMap)
    }

}
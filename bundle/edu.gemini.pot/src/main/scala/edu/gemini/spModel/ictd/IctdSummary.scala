package edu.gemini.spModel.ictd

import scala.collection.JavaConverters._
import scala.collection.immutable.TreeMap

/**
 * All feature and mask availability data extracted from the ICTD in format
 * suitable for use by the QPT and QVis.
 */
final case class IctdSummary(
  featureAvailability: Map[java.lang.Enum[_], Availability],
  maskAvailability:    TreeMap[CustomMaskKey, Availability]
) {

  def featureAvailabilityJava: java.util.Map[java.lang.Enum[_], Availability] =
    featureAvailability.asJava

  def maskAvailabilityJava: java.util.Map[CustomMaskKey, Availability] =
    maskAvailability.asJava

}

object IctdSummary {

  def fromJava(
    f: java.util.Map[java.lang.Enum[_], Availability],
    m: java.util.Map[CustomMaskKey, Availability]
  ): IctdSummary =

    IctdSummary(f.asScala.toMap, TreeMap(m.asScala.toList: _*))

}
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

  /*
  val Feature: String = "feature"
  val Mask:    String = "mask"
  val Entry:   String = "entry"
  val Avail:   String = "avail"
  val Name:    String = "name"
  val Class:   String = "class"

  def apply(params: ParamSet): IctdSummary = {

  }

  private def enumParamSet(factory: PioFactory, name: String, e: Enum[_]): ParamSet = {
    val params = factory.createParamSet(name)
    Pio.addParam(factory, params, Class, e.getClass.getName)
    Pio.addParam(factory, params, Name,  e.name)
    params
  }

  private
  */

}
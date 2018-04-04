package edu.gemini.spModel.ictd

import scalaz._
import Scalaz._

/** Ictd provides access to IctdTracking constructors in a convenient way for
  * Java. Going directly to the IctdTracking companion from Java requires the
  * awkward $.MODULE$ syntax because of the IctdTracking case class.
  */
object Ictd {

  def notTracked(a: Availability): IctdTracking =
    IctdTracking.notTracked(a)

  val installed: IctdTracking =
    IctdTracking.installed

  val unavailable: IctdTracking =
    IctdTracking.unavailable

  def track(name: String): IctdTracking =
    IctdTracking.track(name)

  @annotation.varargs
  def trackAll(name: String, names: String*): IctdTracking =
    IctdTracking.trackAll(name, names: _*)

}
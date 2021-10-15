// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.gemini.visitor

import edu.gemini.model.p1.immutable.Instrument
import edu.gemini.spModel.core.{Angle, Wavelength}
import edu.gemini.spModel.gemini.visitor.VisitorConfig.{DefaultPositionAngle, DefaultWavelength}

import java.time.Duration

import scalaz.Scalaz._
import scalaz._

/**
 * An enumeration of expected visitor instruments and any specialization
 * associated with each.
 */
sealed trait VisitorConfig extends Product with Serializable {

  def instrument: Instrument

  def name: String =
    instrument.display

  def displayValue: String =
    instrument.display

  def wavelength: Wavelength =
    DefaultWavelength

  def positionAngle: Angle =
    DefaultPositionAngle

  def noteTitles: List[String] =
    Nil

  def setupTime: Duration =
    VisitorConfig.DefaultSetupTime

  def readoutTime: Duration =
    VisitorConfig.DefaultReadoutTime

}

object VisitorConfig {

  val DefaultExposureTime: Duration =
    Duration.ofSeconds(0L)

  val DefaultSetupTime: Duration =
    Duration.ofMinutes(10L)

  val DefaultReadoutTime: Duration =
    Duration.ofSeconds(0)

  val DefaultWavelength: Wavelength =
    Wavelength.fromMicrons(0.7)

  val DefaultPositionAngle: Angle =
    Angle.zero

  case object Alopeke extends VisitorConfig {

    override val instrument: Instrument =
      Instrument.Alopeke

    override val wavelength: Wavelength =
      Wavelength.fromMicrons(0.674)

    override val setupTime: Duration =
      Duration.ofMinutes(5L)

    override val readoutTime: Duration =
      Duration.ofSeconds(6L)

  }

  case object Dssi extends VisitorConfig {

    override val instrument: Instrument =
      Instrument.Dssi

    override val wavelength: Wavelength =
      Wavelength.fromMicrons(0.7)

  }

  case object Igrins extends VisitorConfig {

    override val instrument: Instrument =
      Instrument.Igrins

    override val wavelength: Wavelength =
      Wavelength.fromMicrons(2.1)

    override val positionAngle: Angle =
      Angle.fromDegrees(90.0)

    override val noteTitles: List[String] =
      List(
        "IGRINS Observing Details",
        "IGRINS Scheduling Details"
      )

    override val setupTime: Duration =
      Duration.ofMinutes(8L)

    override val readoutTime: Duration =
      Duration.ofSeconds(28L)

  }

    case object MaroonX extends VisitorConfig {

    override val instrument: Instrument =
      Instrument.MaroonX

    override val wavelength: Wavelength =
      Wavelength.fromMicrons(0.7)

    override val setupTime: Duration =
      Duration.ofMinutes(5L)

    override val readoutTime: Duration =
      Duration.ofSeconds(100L)

  }

  case object Zorro extends VisitorConfig {

    override val instrument: Instrument =
      Instrument.Zorro

    override val wavelength: Wavelength =
      Wavelength.fromMicrons(0.674)

    override val setupTime: Duration =
      Duration.ofMinutes(5L)

    override val readoutTime: Duration =
      Duration.ofSeconds(6L)

  }

  case object GenericVisitor extends VisitorConfig {
    override val instrument: Instrument =
      Instrument.Visitor
  }

  implicit val EqVisitorInst: Equal[VisitorConfig] =
    Equal.equalBy(_.name)

  val All: List[VisitorConfig] =
    List(
      Alopeke,
      Dssi,
      Igrins,
      MaroonX,
      Zorro,
      GenericVisitor
    )

  def AllArray: Array[VisitorConfig] =
    All.toArray

  def findByName(name: String): Option[VisitorConfig] =
    All.find(_.name.equalsIgnoreCase(name))

  def findByInstrument(instrument: Instrument): Option[VisitorConfig] =
    All.find(_.instrument == instrument)

}

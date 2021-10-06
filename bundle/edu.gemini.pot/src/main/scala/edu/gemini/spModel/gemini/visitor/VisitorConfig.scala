// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.gemini.visitor

import edu.gemini.spModel.core.{Angle, Wavelength}
import edu.gemini.shared.util.immutable.ScalaConverters._

import java.time.Duration

import scalaz.Scalaz._
import scalaz._

/**
 * An enumeration of expected visitor instruments and any specialization
 * associated with each.
 */
sealed trait VisitorConfig extends Product with Serializable {

  def name: String

  def wavelength: Wavelength =
    Wavelength.zero

  def positionAngle: Angle =
    Angle.zero

  def noteTitles: List[String] =
    Nil

  def setupTime: Duration =
    VisitorConfig.DefaultSetupTime

  def readoutTime: Duration =
    VisitorConfig.DefaultReadoutTime

}

object VisitorConfig {

  def DefaultSetupTime: Duration =
    Duration.ofMinutes(10L)

  def DefaultReadoutTime: Duration =
    Duration.ofSeconds(0)

  case object Alopeke extends VisitorConfig {

    override def name: String = "alopeke"

    override def wavelength: Wavelength =
      Wavelength.fromNanometers(674)

    override def setupTime: Duration =
      Duration.ofMinutes(5L)

    override def readoutTime: Duration =
      Duration.ofSeconds(6L)

  }

  case object Dssi extends VisitorConfig {

    override def name: String = "dssi"

    override def wavelength: Wavelength =
      Wavelength.fromNanometers(700)

  }

  case object Igrins extends VisitorConfig {

    override def name: String = "igrins"

    override def wavelength: Wavelength =
      Wavelength.fromNanometers(2100)

    override def positionAngle: Angle =
      Angle.fromDegrees(90.0)

    override def noteTitles: List[String] =
      List(
        "IGRINS Observing Details",
        "IGRINS Scheduling Details"
      )

    override def setupTime: Duration =
      Duration.ofMinutes(8L)

    override def readoutTime: Duration =
      Duration.ofSeconds(28L)

  }

  case object Zorro extends VisitorConfig {

    override def name: String = "zorro"

    override def wavelength: Wavelength =
      Wavelength.fromNanometers(674)

    override def setupTime: Duration =
      Duration.ofMinutes(5L)

    override def readoutTime: Duration =
      Duration.ofSeconds(6L)

  }

  case object MaroonX extends VisitorConfig {

    override def name: String = "maroonx"

    override def wavelength: Wavelength =
      Wavelength.fromNanometers(700)

    override def setupTime: Duration =
      Duration.ofMinutes(5L)

    override def readoutTime: Duration =
      Duration.ofSeconds(100L)

  }

  implicit val EqVisitorInst: Equal[VisitorConfig] =
    Equal.equalBy(_.name)

  val All: List[VisitorConfig] =
    List(
      Alopeke,
      Dssi,
      Igrins,
      Zorro,
      MaroonX
    )

  def findByName(name: String): Option[VisitorConfig] =
    All.find(_.name.equalsIgnoreCase(name))

  def findByNameJava(name: String): edu.gemini.shared.util.immutable.Option[VisitorConfig] =
    findByName(name).asGeminiOpt

}

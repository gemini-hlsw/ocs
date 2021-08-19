// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.phase2.template.factory.impl.visitor

import edu.gemini.spModel.core.{Angle, Wavelength}

import scalaz._
import Scalaz._

/**
 * An enumeration of expected visitor instruments and any template
 * specialization associated with each.
 */
sealed trait VisitorInst extends Product with Serializable {

  def name: String

  def wavelength: Wavelength =
    Wavelength.zero

  def positionAngle: Angle =
    Angle.zero

  def noteTitles: List[String] =
    Nil

}

object VisitorInst {

  case object Alopeke extends VisitorInst {

    override def name: String = "alopeke"

    override def wavelength: Wavelength =
      Wavelength.fromNanometers(674)

  }

  case object Dssi extends VisitorInst {

    override def name: String = "dssi"

    override def wavelength: Wavelength =
      Wavelength.fromNanometers(700)

  }

  case object Igrins extends VisitorInst {

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

  }

  case object Zorro extends VisitorInst {

    override def name: String = "zorro"

    override def wavelength: Wavelength =
      Wavelength.fromNanometers(674)

  }

  case object MaroonX extends VisitorInst {

    override def name: String = "maroonx"

    override def wavelength: Wavelength =
      Wavelength.fromNanometers(700)

  }

  implicit val EqVisitorInst: Equal[VisitorInst] =
    Equal.equalBy(_.name)

  val All: List[VisitorInst] =
    List(
      Alopeke,
      Dssi,
      Igrins,
      Zorro,
      MaroonX
    )

  def findByName(name: String): Option[VisitorInst] =
    All.find(_.name.equalsIgnoreCase(name))

}

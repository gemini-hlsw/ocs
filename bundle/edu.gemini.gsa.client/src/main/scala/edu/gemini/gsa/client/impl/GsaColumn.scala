package edu.gemini.gsa.client.impl

import java.text.SimpleDateFormat
import java.util.TimeZone
import edu.gemini.model.p1.immutable.{DMS, HMS}

/**
 * Column definition with name and a method to parse a String value into a
 * more appropriately typed value.
 */
sealed trait GsaColumn[T] {
  def name: String
  def parse(value: String): Either[String, T]

  protected def parseVal[T](f: String => T, s: String): Either[String, T] =
    try {
      Right(f(s))
    } catch {
      case _: Exception => Left(couldntParse(s))
    }

  protected def couldntParse(value: String) =
    "Couldn't parse %s value '%s'".format(name, value)
}

object GsaColumn {
  abstract class OptionalStringColumn extends GsaColumn[String] {
    def parse(value: String) = Right(Option(value).getOrElse(""))
  }

  abstract class RequiredStringColumn extends GsaColumn[String] {
    def parse(value: String) =
      Option(value).filter(_.trim != "").toRight("Missing value for column '%s'".format(name))
  }

  abstract class TimeColumn extends GsaColumn[Long] {
    private val dateFormat = new SimpleDateFormat("MMM d yyyy h:mma")
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))

    def parse(value: String): Either[String, Long] = synchronized {
      parseVal(dateFormat.parse(_).getTime, value)
    }
  }

  abstract class CoordinateColumn[T](f: String => T) extends GsaColumn[T] {
    def parse(value: String) = parseVal(f, value.replace(' ', ':'))
  }

  abstract class OptionalValueColumn[T](f: String => T) extends GsaColumn[Option[T]] {
    def parse(value: String) =
      Option(value).filter(_.trim != "") match {
        case None    => Right(None)
        case Some(v) => parseVal(f, v).right.map(Some(_))
      }
  }

  case object DSET_NAME extends RequiredStringColumn {
    val name = "Data Superset Name"
  }
  case object FILE_NAME extends OptionalStringColumn {
    val name = "Original File Name"
  }
  case object SCIENCE_PROG extends OptionalStringColumn {
    val name = "Science Program"
  }
  case object UT_DATE extends TimeColumn {
    val name = "UT Date"
  }
  case object RELEASE_DATE extends TimeColumn {
    val name = "Release Date"
  }
  case object TARGET_NAME extends RequiredStringColumn {
    val name = "Target Name"
  }
  case object RA extends CoordinateColumn(HMS.apply) {
    val name = "RA (J2000)"
  }
  case object DEC extends CoordinateColumn(DMS.apply) {
    val name = "DEC (J2000)"
  }
  case object INSTRUMENT extends RequiredStringColumn {
    val name = "Instrument"
  }
  case object INTEGRATION_TIME extends OptionalValueColumn[Long](sec => (sec.toDouble * 1000).round) {
    val name = "Integration Time"
  }
  case object FILTER extends OptionalStringColumn {
    val name = "Filter(s)"
  }
  case object WAVELENGTH extends OptionalValueColumn[Double](_.toDouble) {
    val name = "Central Wavelength"
  }

  val ALL = List(
    DSET_NAME,
    FILE_NAME,
    SCIENCE_PROG,
    UT_DATE,
    RELEASE_DATE,
    TARGET_NAME,
    RA,
    DEC,
    INSTRUMENT,
    INTEGRATION_TIME,
    FILTER,
    WAVELENGTH
  )
}
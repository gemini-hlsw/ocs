package edu.gemini.model.p1.targetio.table

import edu.gemini.spModel.core.{Declination, RightAscension, MagnitudeSystem, Magnitude}

object Serializers {
  implicit object StringWriter extends StilSerializer[String] {
    def asBinary(value: String) = value
    def primitiveClass = classOf[String]
    def asText(value: String) = value
  }

  implicit object IntWriter extends StilSerializer[Int] {
    def asBinary(value: Int) = value
    def primitiveClass = classOf[java.lang.Integer]
    def asText(value: Int) = value.toString
  }

  implicit object RaWriter extends StilSerializer[RightAscension] {
    def asBinary(value: RightAscension) = value.toAngle.toDegrees
    def primitiveClass = classOf[java.lang.Double]
    def asText(value: RightAscension) = value.toAngle.formatHMS
  }

  implicit object DecWriter extends StilSerializer[Declination] {
    def asBinary(value: Declination) = value.toDegrees
    def primitiveClass = classOf[java.lang.Double]
    def asText(value: Declination) = value.formatDMS
  }

  implicit object OptionalDoubleWriter extends StilSerializer[Option[Double]] {
    def asBinary(value: Option[Double]) = value.getOrElse(Double.NaN)
    def primitiveClass = classOf[java.lang.Double]
    def asText(value: Option[Double]) = value.map("%.3f".format(_)).getOrElse("INDEF")
  }

  implicit object OptionalMagnitudeWriter extends StilSerializer[Option[Magnitude]] {
    private def toDouble(value: Option[Magnitude]): Option[Double] = value.map(_.value)
    def asBinary(value: Option[Magnitude]) = OptionalDoubleWriter.asBinary(toDouble(value))
    def primitiveClass = classOf[java.lang.Double]
    def asText(value: Option[Magnitude]) = OptionalDoubleWriter.asText(toDouble(value))
  }

  implicit object OptionalMagnitudeSystemWriter extends StilSerializer[Option[MagnitudeSystem]] {
    def asBinary(value: Option[MagnitudeSystem]) = value.map(_.name).orNull
    def primitiveClass = classOf[String]
    def asText(value: Option[MagnitudeSystem]) = value.map(_.name).getOrElse("")
  }
}
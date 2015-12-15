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

  implicit object OptionRaWriter extends StilSerializer[Option[RightAscension]] {
    def asBinary(value: Option[RightAscension]) = value.map(_.toAngle.toDegrees).orNull
    def primitiveClass = classOf[java.lang.Double]
    def asText(value: Option[RightAscension]) = value.map(_.toAngle.formatHMS).getOrElse("INDEF")
  }

  implicit object OptionDecWriter extends StilSerializer[Option[Declination]] {
    def asBinary(value: Option[Declination]) = value.map(_.toDegrees).orNull
    def primitiveClass = classOf[java.lang.Double]
    def asText(value: Option[Declination]) = value.map(_.formatDMS).getOrElse("INDEF")
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
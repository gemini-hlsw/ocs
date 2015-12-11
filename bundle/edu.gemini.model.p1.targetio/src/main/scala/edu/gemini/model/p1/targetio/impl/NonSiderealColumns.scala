package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.targetio.table.Readers._
import edu.gemini.model.p1.targetio.table.Serializers._
import edu.gemini.model.p1.targetio.table.{StilSerializer, Column}

import HorizonsEphemerisParser.parseTimeString

private[targetio] object NonSiderealColumns {
  private val readUtc = readString andThen parseTimeString

  val ID     = col("ID",       readInt,            _.ord)
  val NAME   = col("Name",     readString,         _.name)
  val UTC    = col("UTC",      readUtc,            _.element.validAt)(UtcSerializer)
  val RA     = col("RAJ2000",  readRa,             _.element.coords.ra)
  val DEC    = col("DecJ2000", readDec,            _.element.coords.dec)
  val MAG    = col("Mag",      readOptionalDouble, _.element.magnitude)

  val REQUIRED: List[Column[NamedEphemeris, _]] = List(ID, NAME, UTC, RA, DEC)

  private object UtcSerializer extends StilSerializer[Long] {
    def asBinary(value: Long) = asText(value)
    def primitiveClass        = classOf[String]
    def asText(value: Long): String = {
      val res = HorizonsEphemerisParser.Date.format(value)
      val trimmed = res.substring(3)
      if (res.startsWith("BC")) "b"+trimmed else trimmed
    }
  }

  private def col[T](name: String, parse: PartialFunction[Any, T], extract: NamedEphemeris => T)(implicit serializer: StilSerializer[T]) =
    Column[NamedEphemeris, T](name, parse, extract, serializer)
}
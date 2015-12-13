package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.immutable.SiderealTarget

import edu.gemini.model.p1.targetio.table.Readers._
import edu.gemini.model.p1.targetio.table.Serializers._
import edu.gemini.model.p1.targetio.table.{Column, StilSerializer}
import edu.gemini.spModel.core.{MagnitudeBand, Magnitude}

private [targetio] object SiderealColumns {
  val NAME   = col("Name",     readString,         _.name)
  val RA     = col("RAJ2000",  readRa,             a => Some(a.coords.ra))
  val DEC    = col("DecJ2000", readDec,            a => Some(a.coords.dec))
  val PM_RA  = col("pmRA",     readOptionalDouble, _.properMotion.map(_.deltaRA))
  val PM_DEC = col("pmDec",    readOptionalDouble, _.properMotion.map(_.deltaDec))

  val REQUIRED: List[Column[SiderealTarget, _]] = List(NAME, RA, DEC)

  private def extractMagValue[T](target: SiderealTarget, band: MagnitudeBand, f: Magnitude => T): Option[T] =
    target.magnitudes.find(_.band == band).map(f(_))

  def magnitude(band: MagnitudeBand) =
    col(band.name, readOptionalMagnitude(band), target => extractMagValue(target, band, identity))

  def magnitudeSystem(band: MagnitudeBand) =
    col("%s_sys".format(band.name), readOptionalSystem(band), target => extractMagValue(target, band, _.system))

  private def col[T](name: String, parse: PartialFunction[Any, T], extract: SiderealTarget => T)(implicit serializer: StilSerializer[T]) =
    Column[SiderealTarget, T](name, parse, extract, serializer)
}

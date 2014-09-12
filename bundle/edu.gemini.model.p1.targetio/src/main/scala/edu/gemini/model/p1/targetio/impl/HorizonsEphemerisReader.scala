package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.targetio.api._
import edu.gemini.model.p1.immutable._
import java.io.{File, InputStream}

object HorizonsEphemerisReader extends TargetReader[NonSiderealTarget] {
  def read(file: File)      = adapt(HorizonsEphemerisParser.read(file))
  def read(is: InputStream) = adapt(HorizonsEphemerisParser.read(is))
  def read(data: String)    = adapt(HorizonsEphemerisParser.read(data))

  // adapt to the TargetReader.Result
  private def adapt(e: Either[TargetIoError, NonSiderealTarget]): Either[DataSourceError, List[Either[ParseError, NonSiderealTarget]]] =
    e match {
      case Right(t)                  => Right(List(Right(t)))
      case Left(pe: ParseError)      => Right(List(Left(pe)))
      case Left(ds: DataSourceError) => Left(ds)
    }
}
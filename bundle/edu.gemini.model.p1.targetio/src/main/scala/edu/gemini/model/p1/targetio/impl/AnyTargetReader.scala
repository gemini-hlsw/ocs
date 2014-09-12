package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.immutable.Target
import edu.gemini.model.p1.targetio.api.{DataSourceError, TargetReader}
import java.io.{InputStream, File}

/**
 * A TargetReader that just tries all the known formats until it finds one
 * that works, if any.
 */
object AnyTargetReader extends TargetReader[Target] {

  // important to try nonsidereal first since it contains all the required
  // columns for a sidereal target import
  private val readers: List[TargetReader[Target]] = List(NonSiderealReader, HorizonsEphemerisReader, SiderealReader)

  def read(file: File): Result = tryall(_.read(file))
  def read(is: InputStream): Result = tryall(_.read(is))
  def read(data: String): Result = tryall(_.read(data))

  private def tryall[T](f: TargetReader[Target] => Result): Result = {
    val init: Result = Left(DataSourceError(""))
    (init/:readers) {
      case (e, rdr) => if (e.isRight) e else f(rdr)
    }
  }

}
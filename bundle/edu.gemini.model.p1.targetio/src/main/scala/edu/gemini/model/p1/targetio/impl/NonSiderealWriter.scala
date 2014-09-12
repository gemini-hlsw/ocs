package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.immutable.NonSiderealTarget
import edu.gemini.model.p1.targetio.api.{DataSourceError, FileType, TargetWriter}
import java.io.{OutputStream, File}
import edu.gemini.model.p1.targetio.table.{Column, TableWriter}

object NonSiderealWriter extends TargetWriter[NonSiderealTarget] {
  private val all = NonSiderealColumns.REQUIRED ++ List(NonSiderealColumns.MAG)

  private def cols(targets: Traversable[NonSiderealTarget]): List[Column[NamedEphemeris,_]] =
    if (targets.exists(_.ephemeris.exists(_.magnitude.isDefined))) all else NonSiderealColumns.REQUIRED

  def write(targets: Iterable[NonSiderealTarget], file: File, ftype: FileType): Either[DataSourceError, Unit] =
    TableWriter.write(toElements(targets), cols(targets), file, ftype)

  def write(targets: Iterable[NonSiderealTarget], os: OutputStream, ftype: FileType): Either[DataSourceError, Unit] =
    TableWriter.write(toElements(targets), cols(targets), os, ftype)

  private def toElements(targets: Iterable[NonSiderealTarget]): Iterable[NamedEphemeris] =
    targets.zipWithIndex flatMap { case (target, index) =>
      target.ephemeris map { ep => NamedEphemeris(index, target.name, ep) }
    }
}
package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.immutable.SiderealTarget
import edu.gemini.model.p1.mutable.MagnitudeBand
import edu.gemini.model.p1.targetio.api.{TargetWriter, DataSourceError, FileType}
import edu.gemini.model.p1.targetio.table.{TableWriter, Column}

import java.io.{OutputStream, File}

object SiderealWriter extends TargetWriter[SiderealTarget] {
  def write(targets: Iterable[SiderealTarget], file: File, ftype: FileType): Either[DataSourceError, Unit] =
    TableWriter.write(targets, columns(targets), file, ftype)

  def write(targets: Iterable[SiderealTarget], os: OutputStream, ftype: FileType): Either[DataSourceError, Unit] =
    TableWriter.write(targets, columns(targets), os, ftype)

  private def columns(targets: Iterable[SiderealTarget]): List[Column[SiderealTarget, _]] =
    SiderealColumns.REQUIRED ++ pmColumns(targets) ++ bandColumns(targets)

  private def pmColumns(targets: Iterable[SiderealTarget]): List[Column[SiderealTarget, _]] =
    if (targets.exists(_.properMotion.isDefined))
      List(SiderealColumns.PM_RA, SiderealColumns.PM_DEC)
    else
      Nil

  private def bandColumns(targets: Iterable[SiderealTarget]): List[Column[SiderealTarget, _]] =
    bands(targets).toList.sortBy(_.ordinal) flatMap { band =>
      List(SiderealColumns.magnitude(band), SiderealColumns.magnitudeSystem(band))
    }

  private def bands(targets: Iterable[SiderealTarget]) =
    (Set.empty[MagnitudeBand]/:targets) {
      (bandSet, target) => bandSet ++ target.magnitudes.map(_.band)
    }
}
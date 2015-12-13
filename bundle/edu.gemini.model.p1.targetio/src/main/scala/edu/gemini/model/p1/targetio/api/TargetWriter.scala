package edu.gemini.model.p1.targetio.api

import edu.gemini.model.p1.immutable.Target
import java.io.{OutputStream, File}

trait TargetWriter[T <: Target] {
  def write(targets: Iterable[T], file: File, ftype: FileType): Either[DataSourceError, Unit]
  def write(targets: Iterable[T], os: OutputStream, ftype: FileType): Either[DataSourceError, Unit]
}
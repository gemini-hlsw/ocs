package edu.gemini.model.p1.targetio.api

import edu.gemini.model.p1.immutable.Target
import java.io.{InputStream, File}

trait TargetReader[+T <: Target] {
  protected [this] type TargetResult = Either[ParseError, T]
  protected [this] type Result       = Either[DataSourceError, List[TargetResult]]

  def read(file: File): Result
  def read(is: InputStream): Result
  def read(data: String): Result
}
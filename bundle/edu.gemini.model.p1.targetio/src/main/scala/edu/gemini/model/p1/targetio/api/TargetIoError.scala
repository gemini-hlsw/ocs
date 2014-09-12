package edu.gemini.model.p1.targetio.api

sealed trait TargetIoError {
  def msg: String
}

case class ParseError(msg: String, targetName: Option[String], data: Seq[_]) extends TargetIoError

case class DataSourceError(msg: String) extends TargetIoError

object DataSourceError {
  def apply(ex: Exception): DataSourceError = DataSourceError(ex.nonNullMessage)
}
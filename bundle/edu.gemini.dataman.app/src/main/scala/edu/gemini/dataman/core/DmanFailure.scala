package edu.gemini.dataman.core

import edu.gemini.dataman.core.DmanFailure._
import edu.gemini.dataman.query.GsaQueryError

import scalaz._
import Scalaz._

sealed trait DmanFailure {
  def explain: String = this match {
    case QueryFailure(e)   => e.explain
    case Unexpected(msg)   => s"Unexpected error: $msg"
    case DmanException(ex) => s"Unexpected exception: ${ex.getMessage}"
  }

  def exception: Option[Throwable] = this match {
    case QueryFailure(e)  => e.exception
    case DmanException(e) => some(e)
    case _                => none
  }
}

object DmanFailure {
  final case class QueryFailure(e: GsaQueryError) extends DmanFailure
  final case class Unexpected(msg: String)        extends DmanFailure
  final case class DmanException(ex: Throwable)   extends DmanFailure

  implicit val EqualDmanFailure: Equal[DmanFailure] = Equal.equalA
}
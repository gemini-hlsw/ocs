package edu.gemini.ags.servlet

import scalaz._
import Scalaz._

sealed abstract class TargetType(val tag: String) extends Product with Serializable

object TargetType {

  case object Sidereal    extends TargetType("sidereal")
  case object NonSidereal extends TargetType("nonsidereal")

  val all: List[TargetType] =
    List(Sidereal, NonSidereal)

  def fromTag(tag: String): Option[TargetType] =
    all.find(_.tag === tag)

}
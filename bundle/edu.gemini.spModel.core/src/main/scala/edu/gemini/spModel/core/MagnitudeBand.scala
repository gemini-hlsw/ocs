package edu.gemini.spModel.core

import scalaz.Equal

sealed trait MagnitudeBand

object MagnitudeBand {

  case object U  extends MagnitudeBand
  case object B  extends MagnitudeBand
  case object V  extends MagnitudeBand
  case object UC extends MagnitudeBand
  case object R  extends MagnitudeBand
  case object I  extends MagnitudeBand
  case object Y  extends MagnitudeBand
  case object J  extends MagnitudeBand
  case object H  extends MagnitudeBand
  case object K  extends MagnitudeBand
  case object L  extends MagnitudeBand
  case object M  extends MagnitudeBand
  case object N  extends MagnitudeBand
  case object Q  extends MagnitudeBand
 
  implicit val MagnitudeBandEqual: Equal[MagnitudeBand] =
    Equal.equalA

}
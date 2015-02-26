package edu.gemini.pot.sp.validator

import scalaz._

sealed trait NodeCardinality {
  // For Java
  def toInt: Int
}

object NodeCardinality {
  case object Zero extends NodeCardinality {
    def toInt = 0
  }
  case object One extends NodeCardinality {
    def toInt = 1
  }
  case object N extends NodeCardinality {
    def toInt = Int.MaxValue
  }

  implicit def NodeCardinalityEqual: Equal[NodeCardinality] = Equal.equalA
}

package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

sealed trait TargetVisibility {
  def mutable: M.TargetVisibility
  def &(that: TargetVisibility): TargetVisibility
}

object TargetVisibility {
  case object Good extends TargetVisibility {
    def mutable = M.TargetVisibility.GOOD
    def &(that: TargetVisibility) = that
  }

  case object Limited extends TargetVisibility {
    def mutable = M.TargetVisibility.LIMITED
    def &(that: TargetVisibility) = if (that == Bad) that else this
  }

  case object Bad extends TargetVisibility {
    def mutable = M.TargetVisibility.BAD
    def &(that: TargetVisibility) = this
  }

  def values: List[TargetVisibility] = List(Good, Limited, Bad)

  def apply(m: M.TargetVisibility): TargetVisibility =
    values.find(_.mutable == m).get
}
package edu.gemini.spModel.syntax.skycalc

import edu.gemini.skycalc.{Interval, Union}

// Union is fundamentally mutable but we want to treat it as though it were
// immutable with operations that return new Unions.
final class UnionOps[I <: Interval](val self: Union[I]) {
  def modify(f: Union[I] => Unit): Union[I] = {
    val res = self.clone()
    f(res)
    res
  }

  def ∩(that: Union[I]): Union[I] =
    modify(_.intersect(that))

  def +(that: Union[I]): Union[I] =
    modify(_.add(that))

  def -(that: Union[I]): Union[I] =
    modify(_.remove(that))

}

trait ToUnionOps {
  implicit def ToUnionOps[I <: Interval](u: Union[I]): UnionOps[I] =
    new UnionOps(u)
}

object union extends ToUnionOps
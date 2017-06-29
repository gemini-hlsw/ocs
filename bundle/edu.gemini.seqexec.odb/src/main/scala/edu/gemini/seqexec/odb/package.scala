package edu.gemini.seqexec

import edu.gemini.seqexec.odb.SeqFailure.SeqException

import scalaz._, Scalaz._

package object odb {
  type TrySeq[A] = SeqFailure \/ A

  def catchingAll[A](a: => A): TrySeq[A] =
    try {
      a.right
    } catch {
      case ex: Exception => SeqException(ex).left
    }

  def closing[A <: { def close():Unit }, B](a:A)(f: A => B): B =
    try {
      f(a)
    } finally {
      a.close()
    }
}

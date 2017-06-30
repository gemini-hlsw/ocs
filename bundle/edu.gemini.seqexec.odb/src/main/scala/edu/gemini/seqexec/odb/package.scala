package edu.gemini.seqexec

import edu.gemini.seqexec.odb.SeqFailure.SeqException

import scalaz._, Scalaz._

package object odb {
  type TrySeq[A] = SeqFailure \/ A

  def catchingAll[A](a: => A): TrySeq[A] =
    \/.fromTryCatchNonFatal(a).leftMap(SeqException)

  def trySeq[A](a: => TrySeq[A]): TrySeq[A] =
    catchingAll(a).flatMap(identity)

  def closing[A <: { def close():Unit }, B](a:A)(f: A => B): B =
    try {
      f(a)
    } finally {
      a.close()
    }
}

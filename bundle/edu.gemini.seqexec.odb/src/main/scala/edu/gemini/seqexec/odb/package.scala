package edu.gemini.seqexec

import edu.gemini.seqexec.odb.SeqFailure.SeqException
import scala.util.{Failure, Success, Try}

package object odb {
  type TrySeq[A] = Either[SeqFailure, A]

  def catchingAll[A](a: => A): TrySeq[A] =
    Try(a) match {
      case Success(a) => Right(a)
      case Failure(e) => Left(SeqException(e))
    }

  def trySeq[A](a: => TrySeq[A]): TrySeq[A] =
    catchingAll(a).right.flatMap(identity)

  def closing[A <: { def close():Unit }, B](a:A)(f: A => B): B =
    try {
      f(a)
    } finally {
      a.close()
    }
}

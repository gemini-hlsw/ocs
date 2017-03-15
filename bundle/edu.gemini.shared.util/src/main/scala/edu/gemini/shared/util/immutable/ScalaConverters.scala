package edu.gemini.shared.util.immutable

import java.util.function.{Function => JFunction}
import scala.collection.JavaConverters._
import scalaz.{\/-, -\/, \/}

object ScalaConverters {

  implicit class ScalaOptionOps[A](val a: scala.Option[A]) extends AnyVal {
    def asGeminiOpt: Option[A] =
      a.fold[Option[A]](None.instance[A])(new Some(_))
  }

  implicit class ImOptionOps[A](val a: Option[A]) extends AnyVal {
    def asScalaOpt: scala.Option[A] =
      if (a.isEmpty) scala.None else scala.Some(a.getValue)
  }

  implicit class ScalaListOps[A](val a: List[A]) extends AnyVal {
    def asImList: ImList[A] =
      DefaultImList.create(a.asJava)
  }

  implicit class ImListOps[A](val a: ImList[A]) extends AnyVal {
    def asScalaList: List[A] =
      a.toList.asScala.toList
  }

  // N.B. This can't be a value class due to a compiler bug; try again in 2.11
  implicit class ScalaFunction1Ops[T,R](val f: T => R) {
    def asGeminiFunction1: Function1[T,R] =
      new Function1[T,R] {
        def apply(t: T): R = f(t)
      }

    def asJavaFunction1: JFunction[T,R] =
      new JFunction[T,R] {
        def apply(t: T): R = f(t)
      }
  }

  implicit class ImFunction1Ops[T,R](val f: Function1[T,R]) extends AnyVal {
    def asScalaFunction1: T => R =
      f.apply
  }

  // N.B. This can't be a value class due to a compiler bug; try again in 2.11
  implicit class ScalaFunction2Ops[T,U,R](val f: (T,U) => R) {
    def asGeminiFunction2: Function2[T,U,R] =
      new Function2[T,U,R] {
        def apply(t:T, u:U): R = f(t, u)
      }
  }

  implicit class ImFunction2Ops[T,U,R](val f: Function2[T,U,R]) extends AnyVal {
    def asScalaFunction2: (T, U) => R =
      (t, u) => f.apply(t, u)
  }

  implicit class ScalaPairOps[A,B](val p: (A,B)) extends AnyVal {
    def asGeminiPair: Pair[A,B] =
      new Pair[A,B](p._1, p._2)
  }

  implicit class ImPairOps[A,B](val p: Pair[A,B]) extends AnyVal {
    def asScalaPair: (A,B) =
      (p._1(), p._2())
  }

  implicit class ScalaEitherOps[L,R](val e: scala.Either[L,R]) extends AnyVal {
    def asImEither: ImEither[L,R] =
      e.fold(new Left[L,R](_), new Right[L,R](_))
  }

  implicit class ScalazDisjunctionOps[L,R](val e: \/[L,R]) extends AnyVal {
    def asImEither: ImEither[L,R] =
      e.fold(new Left[L,R](_), new Right[L,R](_))
  }

  implicit class ImEitherOps[L,R](val e: ImEither[L,R]) extends AnyVal {
    def asScalaEither: scala.Either[L, R] =
      e.biFold(((l: L) => scala.Left[L,R](l)).asJavaFunction1, ((r: R) => scala.Right[L,R](r)).asJavaFunction1)
    def asScalazDisjunction: \/[L,R] =
      e.biFold(((l: L) => -\/(l)).asJavaFunction1, ((r: R) => \/-(r)).asJavaFunction1)
  }
}
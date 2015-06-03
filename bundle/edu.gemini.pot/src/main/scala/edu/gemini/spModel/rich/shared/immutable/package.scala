package edu.gemini.spModel.rich.shared

import edu.gemini.shared.util.immutable.{ Option, ImList, Function1, Function2, ScalaConverters}

// N.B. this is left behind for compatibility; use edu.gemini.shared.util.immutable.ScalaConverters
package object immutable {
  import ScalaConverters._

  implicit def asGeminiOpt[A](a: scala.Option[A]): ScalaOptionOps[A] =
    new ScalaOptionOps(a)

  implicit def asScalaOpt[A](a: Option[A]): ImOptionOps[A] =
    new ImOptionOps(a)

  implicit def asImList[A](a: List[A]): ScalaListOps[A] =
    new ScalaListOps(a)

  implicit def asScalaList [A](a: ImList[A]): ImListOps[A] =
    new ImListOps(a)

  implicit def asGeminiFunction1[T,R](f: T => R): ScalaFunction1Ops[T,R] =
    new ScalaFunction1Ops(f)

  implicit def asScalaFunction1[T,R](f: Function1[T,R]): ImFunction1Ops[T,R] =
    new ImFunction1Ops(f)

  implicit def asGeminiFunction2[T,U,R](f: (T,U) => R): ScalaFunction2Ops[T,U,R] =
    new ScalaFunction2Ops(f)

  implicit def asScalaFunction2[T,U,R](f: Function2[T,U,R]): ImFunction2Ops[T,U,R] =
    new ImFunction2Ops(f)

}


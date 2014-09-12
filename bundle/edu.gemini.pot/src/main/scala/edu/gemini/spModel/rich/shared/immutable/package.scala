package edu.gemini.spModel.rich.shared

import edu.gemini.shared.util.immutable.{DefaultImList, ImList}
import scala.collection.JavaConverters._

package object immutable {
  // Conversion of Scala Option to lame Gemini Option.
  implicit def asGeminiOpt[A](a: Option[A]) = new Object {
    def asGeminiOpt: edu.gemini.shared.util.immutable.Option[A] = {
      lazy val n: edu.gemini.shared.util.immutable.Option[A] = edu.gemini.shared.util.immutable.None.instance[A]()
      a.fold(n) { aVal => new edu.gemini.shared.util.immutable.Some[A](aVal) }
    }
  }

  // Conversion of lame Gemini Option to Scala Option
  implicit def asScalaOpt[A](a: edu.gemini.shared.util.immutable.Option[A]) = new Object {
    def asScalaOpt: Option[A] = if (a.isEmpty) None else Some(a.getValue)
  }

  // Conversion of Scala List to ImList.
  implicit def asImList[A](a: List[A])  = new Object {
    def asImList: ImList[A] = DefaultImList.create(a.asJava)
  }

  // Conversion of ImList to Scala List.
  implicit def asScalaList[A](a: ImList[A]) = new Object {
    def asScalaList: List[A] = a.toList.asScala.toList
  }

  // Conversion of Gemini FunctionN to Scala FunctionN and vice versa
  implicit def asGeminiFunction1[T,R](f: T => R) = new Object {
    def asGeminiFunction1: edu.gemini.shared.util.immutable.Function1[T,R] = {
      new edu.gemini.shared.util.immutable.Function1[T,R] {
        override def apply(t: T): R = f(t)
      }
    }
  }
  implicit def asScalaFunction1[T,R](f: edu.gemini.shared.util.immutable.Function1[T,R]) = new Object {
    def asScalaFunction1: T => R = {
      t: T => f.apply(t)
    }
  }

  implicit def asGeminiFunction2[T1,T2,R](f: (T1,T2) => R) = new Object {
    def asGeminiFunction2: edu.gemini.shared.util.immutable.Function2[T1,T2,R] = {
      new edu.gemini.shared.util.immutable.Function2[T1,T2,R] {
        override def apply(t1: T1, t2: T2): R = f(t1, t2)
      }
    }
  }
  implicit def asScalaFunction2[T1,T2,R](f: edu.gemini.shared.util.immutable.Function2[T1,T2,R]) = new Object {
    def asScalaFunction1: (T1,T2) => R = {
      (t1: T1, t2: T2)  => f.apply(t1, t2)
    }
  }
}

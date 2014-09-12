package edu.gemini.pit.ui

package object util {

  implicit class pimpIterable[A](val it:Iterable[A]) extends AnyVal {
    def cycle:Iterator[A] = it.iterator ++ cycle
  }
  
}
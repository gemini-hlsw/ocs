package edu.gemini.pit.ui.util

class Glob(pattern: String) {

  private val pat = pattern.toLowerCase.toList

  def matches(s: String): Boolean = matches(s.toLowerCase.toList, pat)

  private def matches(s: Traversable[Char], p: Traversable[Char]): Boolean = (s, p) match {
    case (Nil, Nil)                   => true
    case (_, '*' :: Nil)              => true
    case (a :: as, '?' :: bs)         => matches(as, bs)
    case (a :: as, '*' :: bs)         => matches(as, bs) || matches(a :: as, bs) || matches(as, '*' :: bs)
    case (a :: as, b :: bs) if a == b => matches(as, bs)
    case (_, _)                       => false
  }

}


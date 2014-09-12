package edu.gemini.pit

import scalaz.Monoid

package object ui {
  implicit val boolMonoid = Monoid.instance[Boolean](_ || _,  false)
}

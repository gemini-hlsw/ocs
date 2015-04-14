package edu.gemini.spModel

import scalaz.Equal

package object too {
  implicit val TooTypeEqual: Equal[TooType] = Equal.equalA
}

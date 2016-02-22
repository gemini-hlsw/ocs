package edu.gemini.spModel

import scalaz._
import Scalaz._

package object target {
  implicit val EqualSPTarget: Equal[SPTarget] = Equal.equalRef
}

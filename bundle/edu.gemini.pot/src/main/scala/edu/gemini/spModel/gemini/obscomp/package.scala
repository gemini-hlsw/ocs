package edu.gemini.spModel.gemini

import scalaz.Equal

package object obscomp {

  implicit val EqualActive: Equal[SPProgram.Active] =
    Equal.equalA

}

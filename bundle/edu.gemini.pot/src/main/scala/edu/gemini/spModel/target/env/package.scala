package edu.gemini.spModel.target

import scalaz.OneAnd

package object env {
  type OneAndList[A] = OneAnd[List, A]
}

package edu.gemini.seqexec

import scalaz._

package object odb {
  type TrySeq[A] = SeqFailure \/ A
}

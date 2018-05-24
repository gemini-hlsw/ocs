package edu.gemini.spModel

import edu.gemini.spModel.syntax.event.ToObsExecEventOps
import edu.gemini.spModel.syntax.skycalc.{ToObservingNightOps, ToUnionOps}

package object syntax {
  object all extends ToObsExecEventOps
                with ToObservingNightOps
                with ToUnionOps
}

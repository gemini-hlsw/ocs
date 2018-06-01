package edu.gemini.spModel

import edu.gemini.spModel.syntax.event.ToObsExecEventOps
import edu.gemini.spModel.syntax.skycalc.{ToObservingNightOps, ToUnionOps}
import edu.gemini.spModel.syntax.sp.ToNodeOps

package object syntax {
  object all extends ToNodeOps
                with ToObsExecEventOps
                with ToObservingNightOps
                with ToUnionOps
}

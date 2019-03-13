package edu.gemini.spModel

import edu.gemini.spModel.syntax.event.ToObsExecEventOps
import edu.gemini.spModel.syntax.skycalc.{ToObservingNightOps, ToUnionOps}
import edu.gemini.spModel.syntax.sp.{ToInstrumentOps, ToNodeOps}

package object syntax {
  object all extends ToInstrumentOps
                with ToNodeOps
                with ToObsExecEventOps
                with ToObservingNightOps
                with ToUnionOps
}

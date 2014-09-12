package edu.gemini.sp.vcs.log

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.util.security.principal.GeminiPrincipal

case class VcsEvent(id:Int, op: VcsOp, timestamp:Long, pid:SPProgramID, principals:Set[GeminiPrincipal])


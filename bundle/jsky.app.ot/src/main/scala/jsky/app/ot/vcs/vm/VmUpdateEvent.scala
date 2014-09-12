package jsky.app.ot.vcs.vm

import edu.gemini.pot.sp.version.VersionMap
import edu.gemini.spModel.core.SPProgramID

case class VmUpdateEvent(pid: SPProgramID, vm: Option[VersionMap])
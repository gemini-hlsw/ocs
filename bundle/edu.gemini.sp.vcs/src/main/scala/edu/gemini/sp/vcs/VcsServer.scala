package edu.gemini.sp.vcs

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.sp.version._
import edu.gemini.sp.vcs.VcsFailure._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.sp.vcs.log.VcsEventSet
import java.security.Principal

trait VcsServer {
  def version(id: SPProgramID): TryVcs[VersionMap]
  def fetch(id: SPProgramID): TryVcs[ISPProgram]
  def store(p: ISPProgram): TryVcs[VersionMap]
  def log(p: SPProgramID, offset:Int, length:Int): TryVcs[(List[VcsEventSet], Boolean)]
}


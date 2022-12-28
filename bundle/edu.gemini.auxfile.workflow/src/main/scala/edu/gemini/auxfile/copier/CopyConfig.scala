package edu.gemini.auxfile.copier

import edu.gemini.util.ssh.{SshConfig, DefaultSshConfig}
import edu.gemini.spModel.core.SPProgramID

/**
 * Configuration information needed to scp a file by the AuxFileCopier.
 * This is an abstract class: the getFileType and getDestDir methods must be implemented.
 * User: sraaphor
 */
abstract class CopyConfig(host: String, user: String, password: String, timeout: Int = SshConfig.DEFAULT_TIMEOUT, enabled: Boolean = true)
  extends DefaultSshConfig(host, user, password, timeout, enabled) {
  def getFileType: AuxFileType
  def getDestDir(progId: SPProgramID): String
}

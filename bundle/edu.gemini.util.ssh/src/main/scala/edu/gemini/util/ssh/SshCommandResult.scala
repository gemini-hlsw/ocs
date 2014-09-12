package edu.gemini.util.ssh

/**
 * Result of an ssh command executed on a remote server.
 * @author sraaphor
 */
case class SshCommandResult(exitCode: Int, output: String="", errorMsg: String = "") {
  def success: Boolean = (exitCode == 0)
}

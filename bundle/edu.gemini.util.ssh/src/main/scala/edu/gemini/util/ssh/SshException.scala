package edu.gemini.util.ssh

import com.jcraft.jsch.ChannelSftp

/**
 * Exception thrown by this package. All Java exceptions generated through operations of this package
 * are wrapped in an SshException and then rethrown.
 * @author sraaphor
 */
class SshException(msg: String = null, cause: Throwable = null, private[ssh] val jschID: Int = ChannelSftp.SSH_FX_OK) extends Exception(msg, cause) {
  def this(cause: Throwable) {
    this(cause.getMessage)
  }
}

object SshException {
  def rethrow(wrapped: Throwable) {
    if (wrapped.isInstanceOf[SshException]) throw (wrapped.asInstanceOf[SshException])
    else throw new SshException(wrapped)
  }
}
package edu.gemini.util.ssh

/**
 * A class to hold the configuration information necessary to establish an ssh connection.
 * @author sraaphor
 * @param host     The host to which we want to connect.
 * @param user     The name of the user account on the host for authentication.
 * @param password The password for the user on the host.
 * @param timeout  A timeout for establishing connections and executing commands, in milliseconds.
 */
class DefaultSshConfig(host: String, user: String, password: String, timeout: Int = SshConfig.DEFAULT_TIMEOUT, enabled: Boolean = true) extends SshConfig {
  val getHost = host
  val getUser = user
  val getPassword = password
  val getTimeout = timeout
  val isEnabled = enabled
  override def toString = "%s@%s".format(user, host)
}

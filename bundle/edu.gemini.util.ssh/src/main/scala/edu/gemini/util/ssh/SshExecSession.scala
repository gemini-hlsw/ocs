package edu.gemini.util.ssh

import java.util.logging.Level
import java.util.logging.Logger
import com.jcraft.jsch.{Session, ChannelExec}
import scala.util.{Success, Failure, Try}


/**
 * An ssh session, comprising the connection and methods to execute remote commands.
 * SshExecSessions should ONLY be created through the connect method in the companion object.
 * @author sraaphor
 * @param config The host configuration for establishing the ssh connection.
 */
class SshExecSession private (config: SshConfig, session: Session) {
  assert(session.isConnected, "Session (%s) is not connected".format(config))

  /**
   * Execute a command on the remote server. The session must have already been established and authenticated
   * with a call to connect first.
   * @param command The command to execute remotely.
   * @return An SshCommandResult object encapsulating the result of the attempt to execute the remote command.
   */
  def execute(command: String): Try[SshCommandResult] = {
    if (SshExecSession.LOG.isLoggable(Level.FINE)) {
      SshExecSession.LOG.fine("SshExecSession (%s) execute %s".format(config, command))
    }

    def withChannel(f: ChannelExec => SshCommandResult): SshCommandResult = {
      val channel = session.openChannel("exec").asInstanceOf[ChannelExec]
      try {
        f(channel)
      } finally {
        if (channel.isConnected) channel.disconnect()
      }
    }

    val sshCommandResult = Try {
      withChannel { channel =>
        // Open a channel to perform the command.
        channel.setCommand(command)

        // Get the streams returning information from this channel.
        // The inputStream returns, confusingly, the output from the channel.
        // The errStream returns any error messages from the channel.
        val inputStream = channel.getInputStream
        val errStream   = channel.getErrStream

        // Connect to the channel, which will execute the command.
        channel.connect(config.getTimeout)

        // Now read the channel output and error messages.
        val outputMsg = new String(Stream.continually(inputStream.read).takeWhile(_ != -1).map(_.toByte).toArray)
        val errorMsg  = new String(Stream.continually(errStream.read).takeWhile(_ != -1).map(_.toByte).toArray)

        // Disconnect the channel if necessary and create the command result.
        if (channel.isConnected) channel.disconnect
        new SshCommandResult(channel.getExitStatus, outputMsg, errorMsg)
       }
    }

    sshCommandResult match {
      case Failure(ex) =>
        SshExecSession.LOG.log(Level.WARNING, "SshExecSession (%s) could not execute %s (reason: %s)".format(config, command, ex.getMessage))
        // TODO: Ask Shane if we still want this???
        throw new SshException("SshExecSession (%s) could not execute %s (reason: %s)".format(config, command, ex.getMessage))
      case Success(res) =>
        if (SshExecSession.LOG.isLoggable(Level.FINE)) {
          SshExecSession.LOG.fine("ssh %s:%s result: %s".format(config, command, res))
        }
    }

    sshCommandResult
  }

  /**
   * Disconnect the session.
   * IMPORTANT: If disconnect is NOT called, the code MAY hang. Additionally, a call to any of the methods
   *            in this class after a call to disconnect WILL result in failure.
   */
  def disconnect(): Unit = SshSession.disconnect(config, session)
}

/**
 * The companion object must be used to create SshExecSession objects:
 * 1. A single command can be executed on a remote server using the {@link #execute execute} method.
 * 2. An SshExecSession object can be created (to execute multiple commands on a remote server) using the
 *    {@link #connect connect} method.
 */
object SshExecSession {
  private var LOG: Logger = Logger.getLogger(classOf[SshSession].getName)

  /**
   * A convenience method to create a session, {@link #connect connect}, {@link #execute execute}
   * a given command, and finally {@link #disconnect disconnect}.
   * @param config  The connection configuration information.
   * @param command The command to execute.
   * @return Either an SshCommandResult object indicating success or failure of the command execution, or an exception
   *         if the command could not be executed.
   */
  def execute(config: SshConfig, command: String): Try[SshCommandResult] =
    withSession(config) { execSession =>
      execSession.execute(command)
    }

  /**
   * This is the means by which we create an SshExecSession and connect.
   * @param config The connection configuration information.
   * @return Either a successfully connected SshExecSession or an exception.
   */
  def connect(config: SshConfig): Try[SshExecSession] = {
    // Create a Try[com.jcraft.jsch.Session] object and use it to create a Try[SshExecSession].
    SshSession.connect(config).map(new SshExecSession(config, _))
  }

  /**
   * This method is used by the execute method.
   * @param config The connection configuration information.
   * @param f      A function from an SshExecSession to a SshCommandResult, which runs the desired command on a given
   *               sshExecSession.
   * @return       The result of executing the required instructions / command on an SshExecSession.
   */
  private def withSession(config: SshConfig)(f: SshExecSession => Try[SshCommandResult]): Try[SshCommandResult] =
    // We use flatMap here because connect(config) evaluates to Try[SshExecSession], and then invoking map on this
    // would create a Try[Try[SshCommandResult]]. Using flatMap instead takes this to a Try[SshCommandResult].
    connect(config).flatMap { ssh =>
      try {
        f(ssh)
      } finally {
        ssh.disconnect
      }
    }
}

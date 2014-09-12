package edu.gemini.util.ssh

import java.util.logging.{Level, Logger}
import com.jcraft.jsch.{Session, JSch}
import scala.util.Try

/**
 * Class needed for creating the Logger in the companion object.
 */
private class SshSession {
}

/**
 * Static methods for setting up the basic elements of different type of ssh-based sessions.
 * This companion object is private to the ssh package, like is the case with Java default accessibility.
 * @author sraaphor
 * @param config The configuration information needed to establish a connection to a host and authenticate.
 */
private[ssh] object SshSession {
  private var LOG: Logger = Logger.getLogger(classOf[SshSession].getName)

  /**
   * Establish the connection to the remote sever and authenticate the username / password combination.
   * If a connection has already been established, re-invoking this method generates an exception.
   */
  def connect(config: SshConfig): Try[Session] = {
    // Info-level logging describing the connection.
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("SshSession (%s) connecting".format(config))
    }

    // Turn off strict host key checking: without this, the host must be in ~/.ssh in order
    // to allow a connection through JSch.
    // TODO: We might want to move this to a BundleActivator since it only needs to be done once.
    JSch.setConfig("StrictHostKeyChecking", "no")
    // Create a Try object that contains the session, if we are able to create it.
    // Contains either a Success(Session) or a Failure(exception).
    val trySession = Try {
      val session = new JSch().getSession(config.getUser, config.getHost, 22)
      session.setPassword(config.getPassword)
      session.setTimeout(config.getTimeout)
      session.connect()
      session
    }

    trySession.foreach {_ =>
      if (SshSession.LOG.isLoggable(Level.FINE)) {
        SshSession.LOG.fine("SshSession (%s) connection established".format(config))
      }
    }

    trySession
  }

  /**
   * Disconnect the Session object. Should be called by the specific types of sessions.
   * IMPORTANT: If the Session object IS NOT disconnected, the code MAY hang.
   * @param config  The connection information, for logging purposes.
   * @param session The session to disconnect.
   */
  def disconnect(config: SshConfig, session: Session): Unit = {
    session.disconnect
    if (SshSession.LOG.isLoggable(Level.FINE)) {
      SshSession.LOG.fine("SshSession (%s) disconnected".format(config))
    }
  }
}
package edu.gemini.util.ssh

/**
 * Trait containing all necessary information to connect.
 * User: sraaphor
 */

trait SshConfig {
  def getHost: String
  def getUser: String
  def getPassword: String
  def getTimeout: Int
}

object SshConfig {
  val DEFAULT_TIMEOUT = 5000
}
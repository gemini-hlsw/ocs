package edu.gemini.spModel.rich.pot.spdb

import java.util.logging.{Level, Logger}


/**
 *
 */
trait OdbError {
  def msg: String
  def remoteException: Option[Throwable] = None
}

object OdbError {
  val LOG = Logger.getLogger(getClass.getName)

  /**
   * An ODB error that signifies a problem executing code remotely or calling
   * remote methods.
   */
  class RemoteFailure(ex: Throwable) extends OdbError {
    LOG.log(Level.WARNING, "Problem executing remote function", ex)

    def msg = "Problem executing remote function: %s".format(Option(ex.getMessage).getOrElse(""))
    override def remoteException: Option[Throwable] = Some(ex)
  }
}
package edu.gemini.too.event.client

import edu.gemini.too.event.api.{TooTimestamp, TooService, TooPublisher}
import edu.gemini.util.trpc.client.TrpcClient

import java.util.{Timer, TimerTask}
import java.util.logging.{Level, Logger}

import scala.collection.JavaConverters._
import scalaz._
import edu.gemini.util.security.auth.keychain.KeyChain

/**
 * Polls a remote TooService at a given host and port.
 */
class TooClient(kc: KeyChain, dbHost: String, dbPort: Int, pollPeriodMs: Long) extends TooPublisher {
  private val LOG = Logger.getLogger(classOf[TooClient].getName)

  private val task = new TimerTask {
    var timestamp = Option.empty[TooTimestamp]
    var exception = Option.empty[Exception] // sorry, trying to avoid an exception per poll when the dbHost is down

    private def call[T](op: TooService => T): Option[T] = {

      val remoteService = TrpcClient(dbHost, dbPort).withKeyChain(kc)

      (remoteService { remote => op(remote[TooService]) }) match {
        case \/-(t)  =>
          exception foreach { _ =>
            LOG.info("Good news, now successfully polling %s:%d for ToO events.".format(dbHost, dbPort))
          }
          exception = None
          Some(t)
        case -\/(ex) =>
          if (!exception.exists(_.getClass == ex.getClass)) {
            ex match {
              case ce: java.net.ConnectException =>
                LOG.log(Level.WARNING, "Problem polling %s:%d for ToO events: %s.  Will keep trying ...".format(dbHost, dbPort, ex.getMessage))
              case _ =>
                LOG.log(Level.WARNING, "Problem polling %s:%d for ToO events.  Will keep trying ...".format(dbHost, dbPort), ex)
            }
            exception = Some(ex)
          }
          None
      }
    }

    private def initTimestamp() {
      timestamp = timestamp orElse call(_.lastEventTimestamp())
    }

    def run() {
      initTimestamp()
      timestamp foreach { since =>
        call(_.events(since)) foreach { lst =>
            lst.asScala foreach { evt =>
              timestamp = Some(evt.timestamp)
              publish(evt)
            }
        }
      }
    }
  }

  private var timer = Option.empty[Timer]

  def start() {
    synchronized {
      if (timer.isEmpty) {
        LOG.info("Start polling %s:%d for ToO events.".format(dbHost, dbPort))
        timer = Some(new Timer)
        timer foreach { _.schedule(task, 0, pollPeriodMs)}
      }
    }
  }

  def stop() {
    synchronized {
      timer foreach { _.cancel() }
      timer = None
      LOG.info("Stop polling %s:%d for ToO events.".format(dbHost, dbPort))
    }
  }
}

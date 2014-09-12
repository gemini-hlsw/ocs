package edu.gemini.too.event.api

import edu.gemini.spModel.too.TooType
import java.util.logging.Logger

/**
 * A trait for publishers of TooEvents.  Subscribers may register for all types
 * of ToOs in general, or for one particular types.
 */
trait TooPublisher {
  private val LOG = Logger.getLogger(classOf[TooPublisher].getName)

  private var subscribers: Map[TooType, List[TooSubscriber]] = Map.empty.withDefault(_ => List.empty[TooSubscriber])

  private def forOneType(t: TooType, f: List[TooSubscriber] => List[TooSubscriber]) {
    synchronized {
      subscribers = subscribers.updated(t, f(subscribers(t)))
    }
  }

  private def forAllTypes(f: List[TooSubscriber] => List[TooSubscriber]) {
    synchronized {
      subscribers = (subscribers/:TooType.values().toList) { (m,t) => m.updated(t, f(m(t))) }
    }
  }

  def addTooSubscriber(s: TooSubscriber) { forAllTypes(s :: _) }
  def addTooSubscriber(t: TooType, s: TooSubscriber) { forOneType(t, s :: _) }
  def removeTooSubscriber(s: TooSubscriber) { forAllTypes(_.filterNot(_ == s)) }
  def removeTooSubscriber(t: TooType, s: TooSubscriber) { forOneType(t, _.filterNot(_ == s)) }

  protected def publish(e: TooEvent) {
    LOG.info("Publishing ToO event: " + e)

    // the subscribers reference is mutable but the map itself is not so
    // we're free to access it without locking or copies
    subscribers(e.tooType) foreach { s => s.tooObservationReady(e) }
  }
}
package edu.gemini.too.email.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.too.event.api.TooPublisher
import edu.gemini.util.osgi.Tracker._

import org.osgi.framework.{BundleContext, BundleActivator}
import org.osgi.util.tracker.ServiceTracker
import edu.gemini.too.email.TooEmailSender

class Activator extends BundleActivator {
  private var tracker: Option[ServiceTracker[_,_]] = None

  override def start(ctx: BundleContext) {
    println("Start edu.gemini.too.email")
    val config = new OsgiEmailConfig(ctx)

    tracker = Some(track[IDBDatabaseService, TooPublisher, (() => Unit)](ctx) { (odb, pub) =>
      val sender = new TooEmailSender(config, odb)
      pub.addTooSubscriber(sender)

      // Cleanup
      () => { pub.removeTooSubscriber(sender) }
    } { _.apply() })

    tracker.foreach(_.open())
  }

  override def stop(ctx: BundleContext) {
    println("Stop edu.gemini.too.email")
    tracker.foreach(_.close())
    tracker = None
  }
}

package edu.gemini.too.window.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.too.event.api.TooPublisher
import edu.gemini.too.window.TooWindowChecker
import edu.gemini.util.osgi.Tracker._
import org.osgi.framework.{BundleActivator, BundleContext}
import org.osgi.util.tracker.ServiceTracker
import java.security.Principal
import edu.gemini.util.security.principal.StaffPrincipal

/**
 *
 */
class Activator extends BundleActivator {

  private var tracker: Option[ServiceTracker[_,_]] = None

  // Superuser
  private val user = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  override def start(ctx: BundleContext) {
    println("Start edu.gemini.too.window")

    tracker = Some(track[IDBDatabaseService, TooPublisher, (() => Unit)](ctx) { (odb, pub) =>
      val win = new TooWindowChecker(odb, user)
      pub.addTooSubscriber(win)

      // Cleanup
      () => { pub.removeTooSubscriber(win) }
    } { _.apply() })

    tracker.foreach(_.open())
  }

  override def stop(ctx: BundleContext) {
    println("Stop edu.gemini.too.window")
    tracker.foreach(_.close())
    tracker = None
  }
}


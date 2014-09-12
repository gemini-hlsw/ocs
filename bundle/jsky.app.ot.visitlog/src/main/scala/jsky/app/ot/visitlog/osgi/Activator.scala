package jsky.app.ot.visitlog.osgi

import jsky.app.ot.plugin.{OtViewerService, OtActionPlugin}
import jsky.app.ot.visitlog.{VisitLogPanel, ShowVisitLogAction}

import edu.gemini.util.osgi.Tracker._

import org.osgi.framework.{ServiceRegistration, BundleActivator, BundleContext}
import org.osgi.util.tracker.ServiceTracker

class Activator extends BundleActivator {
  private var reg: Option[ServiceRegistration[OtActionPlugin]] = None

  private var tracker: ServiceTracker[_,_] = null

  def start(ctx: BundleContext): Unit = {
    reg = Option(ctx.registerService(classOf[OtActionPlugin], new ShowVisitLogAction, new java.util.Hashtable[String, Object]()))

    tracker = track[OtViewerService,Unit](ctx) { vs =>
      VisitLogPanel.viewerService = Some(vs)
    } { _ => VisitLogPanel.viewerService = None }
    tracker.open()
  }

  def stop(ctx: BundleContext): Unit = {
    reg.foreach(_.unregister())
    reg = None
    tracker.close()
    tracker = null
  }
}

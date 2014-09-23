package edu.gemini.p1monitor.osgi

import org.osgi.framework.{BundleContext, BundleActivator}
import org.osgi.service.http.{NamespaceException, HttpService}
import java.util
import edu.gemini.p1monitor.fetch.FetchServlet
import edu.gemini.p1monitor.P1MonitorDirMonitor
import util.Hashtable
import util.logging.{Level, Logger}
import edu.gemini.p1monitor.config.P1MonitorConfig
import javax.servlet.ServletException
import edu.gemini.util.osgi.Tracker._
import org.osgi.util.tracker.ServiceTracker

class Activator extends BundleActivator {
  val LOG = Logger.getLogger(this.getClass.getName)

  var monitor: P1MonitorDirMonitor = _
  var tracker: Option[ServiceTracker[HttpService, HttpService]] = None

  override def start(context: BundleContext) {
    val config: P1MonitorConfig = new P1MonitorConfig(context)
    monitor = new P1MonitorDirMonitor(config)

    tracker = Some(track[HttpService, HttpService](context) { httpRef: HttpService =>
        try {
          httpRef.registerServlet("/fetch", new FetchServlet(config), new Hashtable[Any, Any](), null)
          monitor.startMonitoring()
        } catch {
          case ex: ServletException => LOG.log(Level.SEVERE, "Trouble setting up web application.", ex)
          case ex: NamespaceException => LOG.log(Level.SEVERE, "Trouble setting up web application.", ex)
        }

        httpRef
      } {
        x: HttpService =>
          monitor.stopMonitoring()
      })
    tracker.foreach(_.open())
  }

  override def stop(context: BundleContext) {
    monitor.stopMonitoring()
    tracker.foreach(_.close())
    tracker = None
  }

}
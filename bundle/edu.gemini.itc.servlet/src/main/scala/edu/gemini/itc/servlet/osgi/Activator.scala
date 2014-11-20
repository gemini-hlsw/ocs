package edu.gemini.itc.servlet.osgi

import edu.gemini.util.osgi.Tracker._

import java.util
import java.util.logging.Logger
import javax.servlet.http.HttpServlet

import org.osgi.framework.{BundleContext, BundleActivator}
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.ServiceTracker

object Activator {
  val Log = Logger.getLogger(this.getClass.getName)
  val AppContext = "/itc"
}

import edu.gemini.itc.servlet.osgi.Activator._

class Activator extends BundleActivator {

  private var st: ServiceTracker[_,_] = null

  override def start(ctx: BundleContext): Unit = {
    Log.info(s"Start ${this.getClass.getPackage.getName}")

    st = track[HttpService, HttpService](ctx) { (http) =>
      http.registerServlet(AppContext, new DummyItcServlet, new util.Hashtable(), null)
      http
    } {
      http => http.unregister(AppContext)
    }

    st.open()
  }

  override def stop(ctx: BundleContext): Unit = {
    Log.info(s"Stop ${this.getClass.getPackage.getName}")
    st.close()
    st = null
  }
}

final class DummyItcServlet extends HttpServlet


package edu.gemini.spdb.authServlet.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spdb.authServlet.AuthServlet
import edu.gemini.util.osgi.Tracker._
import edu.gemini.util.security.auth.keychain.KeyService
import org.osgi.framework.{BundleActivator, BundleContext}
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.ServiceTracker

import java.util.logging.Logger

object Activator {
  val Log = Logger.getLogger(this.getClass.getName)
  val AppContext = "/auth"
}

import Activator._

class Activator extends BundleActivator {

  private var st: ServiceTracker[_,_] = null

  override def start(ctx: BundleContext): Unit = {
    Log.info(s"Start ${this.getClass.getPackage.getName}")

    st = track[KeyService, HttpService, IDBDatabaseService, HttpService](ctx) { (key, http, db) =>
      http.registerServlet(AppContext, new AuthServlet(key, db), new java.util.Hashtable[String, Object](), null)
      http
    } { http => http.unregister(AppContext) }

    st.open()
  }

  override def stop(ctx: BundleContext): Unit = {
    Log.info(s"Stop ${this.getClass.getPackage.getName}")
    st.close()
    st = null
  }
}


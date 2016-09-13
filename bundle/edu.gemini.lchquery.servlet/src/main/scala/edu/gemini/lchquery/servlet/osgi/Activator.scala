package edu.gemini.lchquery.servlet.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.util.osgi.Tracker._

import org.osgi.framework.{BundleContext, BundleActivator}
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.ServiceTracker

import java.util.logging.Logger
import edu.gemini.lchquery.servlet.LchQueryServlet
import java.util.Hashtable
import java.security.Principal
import edu.gemini.util.security.principal.StaffPrincipal

import scala.collection.JavaConversions._

/**
 *
 */
object Activator {
  val Log = Logger.getLogger(getClass.getName)
  val AppContext = "/odbbrowser"
}

import Activator._

final class Activator extends BundleActivator {

  private var tracker: ServiceTracker[_,_] = null

  // We will run as superuser
  val user = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  def start(ctx: BundleContext): Unit = {
    Log.info("Start LCH ODB Query Service")
    tracker = track[IDBDatabaseService, HttpService, HttpService](ctx) { (odb, http) =>
      Log.info(s"Registering $AppContext servlet")
      http.registerServlet(AppContext, LchQueryServlet(odb, user.toSet), new Hashtable(), null)
      http
    } { http =>
      http.unregister(AppContext)
    }

    tracker.open()
  }

  def stop(ctx: BundleContext): Unit = {
    Log.info("Stop LCH ODB Query Service")
    tracker.close()
    tracker = null
  }

}
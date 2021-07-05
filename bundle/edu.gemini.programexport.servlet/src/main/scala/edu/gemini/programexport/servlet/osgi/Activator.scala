package edu.gemini.programexport.servlet.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.programexport.servlet.ProgramExportServlet
import edu.gemini.util.osgi.Tracker._
import org.osgi.framework.{BundleActivator, BundleContext}
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.ServiceTracker

import java.util.logging.Logger
import java.util.{Hashtable => JHashtable, Set => JSet}
import java.security.Principal
import edu.gemini.util.security.principal.StaffPrincipal

import scala.collection.JavaConversions._


object Activator {
  val Log: Logger = Logger.getLogger(getClass.getName)
  val AppContext = "/programexport"
}

import Activator._

final class Activator extends BundleActivator {

  private var tracker: ServiceTracker[_,_] = null

  // Run as superuser.
  val user: JSet[Principal] = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  def start(ctx: BundleContext): Unit = {
    Log.warning(s"Start Program Export Service: $AppContext")
    tracker = track[IDBDatabaseService, HttpService, HttpService](ctx) { (odb, http) =>
      Log.warning(s"Registering $AppContext servlet")
      http.registerServlet(AppContext, ProgramExportServlet(odb, user.toSet), new JHashtable(), null)
      http
    } { http =>
      http.unregister(AppContext)
    }

    tracker.open()
  }

  def stop(ctx: BundleContext): Unit = {
    Log.warning(s"Stop Program Export Service: $AppContext")
    tracker.close()
    tracker = null
  }
}
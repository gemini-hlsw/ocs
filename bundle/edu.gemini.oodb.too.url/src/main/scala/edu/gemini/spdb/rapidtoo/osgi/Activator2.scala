package edu.gemini.spdb.rapidtoo.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import org.osgi.service.http.HttpService
import edu.gemini.util.security.auth.keychain.KeyService
import org.osgi.framework.{BundleContext, BundleActivator}
import java.util.logging.Logger
import org.osgi.util.tracker.ServiceTracker
import edu.gemini.util.osgi.Tracker._
import edu.gemini.spdb.rapidtoo.www.TooUpdateServlet
import java.util.Hashtable
import java.security.Principal
import edu.gemini.util.security.principal.StaffPrincipal

class Activator extends BundleActivator {

  private val Log = Logger.getLogger(getClass.getName())
  private val Alias = "/too"
  
  private var tracker: ServiceTracker[_,_] = null

  // We run as the superuser
  private val user = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  override def start(ctx: BundleContext) {
    tracker = track[IDBDatabaseService, HttpService, KeyService, HttpService](ctx) { (db, http, ks) =>
      Log.info("Adding rapid TOO HttpService");
      http.registerServlet(Alias, new TooUpdateServlet(db, ks, user), new Hashtable[Any,Any](), null);
      http
    } { _.unregister(Alias) }
    tracker.open()
  }

  override def stop(ctx: BundleContext) {
    tracker.close()
    tracker = null
  }
}

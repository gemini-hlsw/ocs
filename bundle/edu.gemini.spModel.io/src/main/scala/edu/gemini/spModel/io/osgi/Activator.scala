package edu.gemini.spModel.io.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.io.ocs3.Ocs3ExportServlet
import edu.gemini.util.osgi.Tracker._

import org.osgi.framework.{BundleContext, BundleActivator}
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.ServiceTracker

import java.util.logging.Logger

object Activator {
  val Context = "/ocs3"
  val Log = Logger.getLogger(classOf[Activator].getName)
}

import Activator._

final class Activator extends BundleActivator {

  private var st: Option[ServiceTracker[_,_]] = None

  private val name = this.getClass.getPackage.getName

  override def start(ctx: BundleContext): Unit = {
    Log.info(s"Start $name")

    st = Some(track[HttpService, IDBDatabaseService, HttpService](ctx) { (http, db) =>
      http.registerServlet(Context, new Ocs3ExportServlet(db), new java.util.Hashtable[String, Object](), null)
      http
    } { _.unregister(Context) })

    st.foreach(_.open())
  }

  override def stop(ctx: BundleContext): Unit = {
    Log.info(s"Stop $name")
    st.foreach(_.close())
    st = None
  }
}


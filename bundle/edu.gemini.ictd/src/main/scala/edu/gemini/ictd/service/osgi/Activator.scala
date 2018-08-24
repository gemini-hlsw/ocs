package edu.gemini.ictd.service.osgi

import edu.gemini.ictd.IctdDatabase
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.ictd.IctdService
import edu.gemini.util.osgi.SecureServiceFactory
import edu.gemini.util.osgi.SecureServiceFactory._
import edu.gemini.util.osgi.Tracker._

import org.osgi.framework.{ Bundle, BundleActivator, BundleContext, ServiceRegistration }
import org.osgi.util.tracker.ServiceTracker

import java.security.Principal
import java.util.logging.Logger

import scalaz._
import Scalaz._

/**
 * An activator for the ICTD bundle that creates IctdService instances on
 * demand.
 */
class Activator extends BundleActivator {
  import Activator._

  private var tracker: Option[ServiceTracker[_,_]] = None

  override def start(ctx: BundleContext): Unit = {

    // Extract the named property value or throw an exception if not present.
    def prop(name: String): String =
      Option(ctx.getProperty(name)).getOrElse {
        val msg = s"Missing bundle property $name"
        Log.severe(msg)
        sys.error(msg)
      }

    tracker = Mode(ctx) match {

      case Api     =>
        Log.info("Starting edu.gemini.ictd in Api mode")
        None

      case Service =>
        Log.info("Starting edu.gemini.ictd in Service mode")

        val user = prop(IctdUserProp)
        val pass = prop(IctdPassProp)
        val gn   = IctdDatabase.Configuration(prop(IctdGnProp), user, pass)
        val gs   = IctdDatabase.Configuration(prop(IctdGsProp), user, pass)

        val t = track[IDBDatabaseService, (() => Unit)](ctx) { odb =>
          val factory = new SecureServiceFactory[IctdService] {
            def getService(ps: Set[Principal]): IctdService =
              IctdServiceImpl.service(odb, gn, gs, ps)
          }

          Log.info("Registering IctdService")
          val reg = ctx.registerSecureService(factory, Map("trpc" -> ""))

          () => reg.unregister()
        } { _.apply() }

        Some(t)
    }

    tracker.foreach(_.open())
  }

  override def stop(ctx: BundleContext): Unit = {
    Log.info("Stopping edu.gemini.ictd")
    tracker.foreach(_.close())
    tracker = None
  }
}

object Activator {
  val Log: Logger = Logger.getLogger(classOf[Activator].getName)

  val IctdModeProp: String = "edu.gemini.ictd.mode"
  val IctdPassProp: String = "edu.gemini.ictd.password"
  val IctdUserProp: String = "edu.gemini.ictd.user"
  val IctdGnProp: String   = "edu.gemini.ictd.gn"
  val IctdGsProp: String   = "edu.gemini.ictd.gs"

  /**
   * Bundle mode, either "api" or "service".  If "api" nothing is started or
   * registered (this is the default).  If "service" then register a secure
   * service factory for creating IctdService on demand.
   */
  sealed abstract class Mode(val name: String)
  case object Api     extends Mode("api")
  case object Service extends Mode("service")

  val AllModes = List(Api, Service)

  object Mode {
    def apply(ctx: BundleContext): Mode =
      Option(ctx.getProperty(IctdModeProp)).flatMap(s => AllModes.find(_.name === s)) | Api
  }

}

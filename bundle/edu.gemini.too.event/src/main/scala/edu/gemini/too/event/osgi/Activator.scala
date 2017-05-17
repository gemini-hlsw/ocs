package edu.gemini.too.event.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.too.event.api.{TooService => TooServiceApi, TooPublisher}
import edu.gemini.too.event.osgi.TooServiceMode.Service
import edu.gemini.too.event.service._
import edu.gemini.util.osgi.Tracker._

import org.osgi.framework.{ServiceRegistration, Bundle, BundleActivator, BundleContext}
import org.osgi.util.tracker.ServiceTracker
import edu.gemini.spModel.core.osgi.SiteProperty
import edu.gemini.util.osgi.SecureServiceFactory
import edu.gemini.util.osgi.SecureServiceFactory._
import java.security.Principal

import scala.collection.JavaConverters._

class Activator extends BundleActivator {
  private var tracker: Option[ServiceTracker[_,_]] = None

  // The TooService is published under two interfaces.  TooServiceApi for use
  // by remote clients via Trpc and TooPublisher for use by local clients in
  // the same process.

  override def start(ctx: BundleContext) {
    tracker = (TooServiceMode(ctx), Option(SiteProperty.get(ctx))) match {
      case (Service, Some(site)) =>
        println("Starting edu.gemini.too.event bundle in Service mode for site %s".format(site.displayName))
        Some(track[IDBDatabaseService, (() => Unit)](ctx) { odb =>

          // Our TOO service, registered locally as a TooPublisher
          val service = new TooService(odb, site)
          odb.addProgramEventListener(service)
          val props1 = new java.util.Hashtable[String,String]
          val reg1 = ctx.registerService(classOf[TooPublisher].getName, service, props1)

          // Our TooServiceApi, for TRPC clients
          val factory = new SecureServiceFactory[TooServiceApi] {
            def getService(ps: Set[Principal]): TooServiceApi =
              service.serviceApi(ps.asJava)
          }
          val reg2 = ctx.registerSecureService(factory, Map("trpc" -> ""))

          // Cleanup
          () => {
            odb.removeProgramEventListener(service)
            reg1.unregister()
            reg2.unregister()
          }
        } { _.apply() })

      case (Service, None) =>
        println("Could not start edu.gemini.too.event bundle in Service mode because the '%s' property was not specified".format(SiteProperty.NAME))
        None

      case _ =>
        println("Starting edu.gemini.too.event bundle in Client mode.")
        None
    }

    tracker.foreach(_.open())
  }

  override def stop(ctx: BundleContext) {
    println("Stopping edu.gemini.too.event bundle.")
    tracker.foreach(_.close())
    tracker = None
  }
}

